// useWebSocket.js
import { useState, useEffect, useRef } from 'react';

export const useWebSocket = (url) => {
    const [socket, setSocket] = useState(null);
    const [lastMessage, setLastMessage] = useState(null);
    const [connectionStatus, setConnectionStatus] = useState('Connecting');

    useEffect(() => {
        const ws = new WebSocket(`ws://localhost:8080${url}`);

        ws.onopen = () => {
            setConnectionStatus('Connected');
            setSocket(ws);
        };

        ws.onmessage = (event) => {
            setLastMessage(event);
        };

        ws.onclose = () => {
            setConnectionStatus('Disconnected');
        };

        return () => ws.close();
    }, [url]);

    const sendMessage = (message) => {
        if (socket && socket.readyState === WebSocket.OPEN) {
            socket.send(message);
        }
    };

    return { sendMessage, lastMessage, connectionStatus };
};