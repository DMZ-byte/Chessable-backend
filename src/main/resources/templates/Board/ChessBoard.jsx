// ChessBoard.jsx
import React, { useState, useEffect } from 'react';
import { useWebSocket } from '../hooks/useWebSocket';

const ChessBoard = ({ gameId, playerId, playerColor }) => {
    const [board, setBoard] = useState(initialBoard);
    const [selectedSquare, setSelectedSquare] = useState(null);
    const [gameState, setGameState] = useState('active');

    const { sendMessage, lastMessage } = useWebSocket(`/websocket/game`);

    useEffect(() => {
        if (lastMessage) {
            const data = JSON.parse(lastMessage.data);
            setBoard(fenToBoard(data.fen));
            setGameState(data.gameStatus);
        }
    }, [lastMessage]);

    const handleSquareClick = (row, col) => {
        if (selectedSquare) {
            const move = algebraicNotation(selectedSquare, {row, col}, board);
            if (move) {
                sendMessage(JSON.stringify({
                    gameId,
                    playerId,
                    move
                }));
            }
            setSelectedSquare(null);
        } else {
            setSelectedSquare({row, col});
        }
    };

    return (
        <div className="chess-board">
            {board.map((row, rowIndex) => (
                <div key={rowIndex} className="board-row">
                    {row.map((piece, colIndex) => (
                        <Square
                            key={`${rowIndex}-${colIndex}`}
                            piece={piece}
                            isSelected={selectedSquare?.row === rowIndex && selectedSquare?.col === colIndex}
                            onClick={() => handleSquareClick(rowIndex, colIndex)}
                            isLight={(rowIndex + colIndex) % 2 === 0}
                        />
                    ))}
                </div>
            ))}
        </div>
    );
};