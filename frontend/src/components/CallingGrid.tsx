import { useState } from 'react';
import PlayingCard from './PlayingCard';

const SUIT_CODES = ['c', 'd', 'h', 's'];
const RANK_CODES = ['2', '3', '4', '5', '6', '7', '8', '9', 'T', 'J', 'Q', 'K', 'A'];

// Every possible card code, grouped by suit -- e.g. "2c","3c",...,"Ac","2d",...
// Verify this matches your actual encoding convention (checked against your
// hand codes like "Jc"/"Ah") before trusting this list.
const ALL_CARDS: string[] = SUIT_CODES.flatMap((suit) =>
    RANK_CODES.map((rank) => `${rank}${suit}`)
);

type CallingGridProps = {
    yourTurn: boolean;
    onConfirmCall: (card: string) => void;
};

export default function CallingGrid({ yourTurn, onConfirmCall }: CallingGridProps) {
    const [selectedCode, setSelectedCode] = useState<string | null>(null);

    function confirm() {
        if (!yourTurn || selectedCode === null) return; // defensive; button should already be disabled
        onConfirmCall(selectedCode);
        setSelectedCode(null);
    }

    return (
        <div>
            <div
                style={{
                    display: 'grid',
                    gridTemplateColumns: `repeat(${RANK_CODES.length}, auto)`,
                    gap: '4px',
                }}
            >
                {ALL_CARDS.map((code) => (
                    <PlayingCard
                        key={code}
                        code={code}
                        width={48}
                        selected={code === selectedCode}
                        onClick={
                            yourTurn
                                ? () => setSelectedCode(code === selectedCode ? null : code)
                                : undefined
                        }
                    />
                ))}
            </div>

            <button
                disabled={!yourTurn || selectedCode === null}
                onClick={confirm}
                style={{ marginTop: '8px' }}
            >
                Confirm Partner Call
            </button>
        </div>
    );
}