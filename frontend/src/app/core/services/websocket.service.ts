import { Injectable } from '@angular/core';
import { Client, IMessage } from '@stomp/stompjs';
import { Observable, Subject } from 'rxjs';
import { environment } from '../../../environments/environment';
import { MessageDto } from './api.service';

@Injectable({
  providedIn: 'root'
})
export class WebSocketService {
  private stompClient: Client | null = null;
  private messageSubject = new Subject<MessageDto>();

  /**
   * Connecte le client STOMP et écoute les messages de l'utilisateur.
   */
  connect(userEmail: string): Observable<MessageDto> {
    if (this.stompClient && this.stompClient.active) {
      return this.messageSubject.asObservable();
    }

    const wsUrl = environment.wsUrl ? environment.wsUrl.replace('http', 'ws') : environment.apiUrl.replace(/^http/, 'ws') + '/ws-whispr';

    this.stompClient = new Client({
      brokerURL: wsUrl,
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      debug: (str) => {
        // Mode debug optionnel
      }
    });

    this.stompClient.onConnect = () => {
      this.stompClient?.subscribe(`/topic/user/${userEmail}/messages`, (msg: IMessage) => {
        try {
          if (msg.body) {
            const newMsg: MessageDto = JSON.parse(msg.body);
            this.messageSubject.next(newMsg);
          }
        } catch (e) {
          console.error('Erreur parsing websocket msg', e);
        }
      });
    };

    this.stompClient.onStompError = (frame) => {
      console.error('Erreur Broker STOMP: ' + frame.headers['message']);
    };

    this.stompClient.activate();
    return this.messageSubject.asObservable();
  }

  /**
   * Déconnecte le client STOMP.
   */
  disconnect(): void {
    if (this.stompClient) {
      this.stompClient.deactivate();
      this.stompClient = null;
    }
  }
}
