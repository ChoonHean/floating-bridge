import PlayingCard from './PlayingCard';

const RELATIVE_POSITIONS = ['bottom', 'left', 'top', 'right'] as const;
type Position = (typeof RELATIVE_POSITIONS)[number];

function positionFor(actualSeat: number, yourSeat: number): Position {
    const offset = (actualSeat - yourSeat + 4) % 4;
    return RELATIVE_POSITIONS[offset];
}

type TableProps = {
    yourSeat: number;
    players: (string | null)[];
    handLengths: number[];
    played: { first: number; second: string }[];
    tricks: number[];
    currentTurn: number;
};

export default function Table({ yourSeat, players, handLengths, played, tricks, currentTurn }: TableProps) {
    const playedBySeat = new Map<number, string>(played.map((p) => [p.first, p.second]));

    const seats = [0, 1, 2, 3];

    return (
        <div className="table-grid">
            {seats.map((actualSeat) => {
                const position = positionFor(actualSeat, yourSeat);
                const isYou = actualSeat === yourSeat;
                const playedCard = playedBySeat.get(actualSeat);

                return (
                    <div key={actualSeat} className={`seat seat-${position}`}>
                        <div className="seat-label">
                            {players[actualSeat]}
                            {actualSeat === currentTurn && ' (turn)'}
                            {`(${tricks[actualSeat]} sets)`}
                        </div>
                        {!isYou && <div className="hand-count">{handLengths[actualSeat]} cards</div>}
                        {playedCard && (
                            <div className="played-card">
                                <PlayingCard code={playedCard} width={56} selected={false} />
                            </div>
                        )}
                    </div>
                );
            })}
        </div>
    );
}