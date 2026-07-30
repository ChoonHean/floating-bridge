import { useState } from 'react';

type Bid = { suit: string; value: number };

const SUITS = ['CLUBS', 'DIAMONDS', 'HEARTS', 'SPADES', 'NT'];
const SUIT_SYMBOLS: Record<string, string> = {
    CLUBS: '♣',
    DIAMONDS: '♦',
    HEARTS: '♥',
    SPADES: '♠',
    NT: 'NT',
};
const SUIT_RANK: Record<string, number> = {
    CLUBS: 0,
    DIAMONDS: 1,
    HEARTS: 2,
    SPADES: 3,
    NT: 4,
};
const LEVELS = [1, 2, 3, 4, 5, 6, 7];

function isValid(currentBid: Bid | null, level: number, suit: string): boolean {
    if (currentBid === null) return true;
    if (level !== currentBid.value) return level > currentBid.value;
    return SUIT_RANK[suit] > SUIT_RANK[currentBid.suit];
}

function isSameBid(a: Bid | null, level: number, suit: string): boolean {
    return a !== null && a.suit === suit && a.value === level;
}

type BiddingGridProps = {
    currentBid: Bid | null;
    yourTurn: boolean;
    onConfirmBid: (bid: Bid | null) => void; // null = pass
};

export default function BiddingGrid({ currentBid, yourTurn, onConfirmBid }: BiddingGridProps) {
    const [selected, setSelected] = useState<Bid | null>(null);
    // Distinguishes "no button chosen yet" from "Pass explicitly chosen" --
    // both involve `selected === null`, so a plain boolean flag alongside it
    // is simpler than trying to encode "pass" as a fake Bid value.
    const [passSelected, setPassSelected] = useState(false);

    function selectBid(level: number, suit: string) {
        setPassSelected(false);
        setSelected({ suit, value: level });
    }

    function selectPass() {
        setSelected(null);
        setPassSelected(true);
    }

    function confirm() {
        if (!yourTurn) return; // defensive; button should already be disabled
        onConfirmBid(passSelected ? null : selected);
        setSelected(null);
        setPassSelected(false);
    }

    const hasSelection = selected !== null || passSelected;

    return (
        <div>
            <table style={{ borderCollapse: 'collapse' }}>
                <thead>
                    <tr>
                        <th></th>
                        {SUITS.map((suit) => (
                            <th key={suit} style={{ padding: '4px 8px' }}>
                                {SUIT_SYMBOLS[suit]}
                            </th>
                        ))}
                    </tr>
                </thead>
                <tbody>
                    {LEVELS.map((level) => (
                        <tr key={level}>
                            <td style={{ padding: '4px 8px', fontWeight: 'bold' }}>{level}</td>
                            {SUITS.map((suit) => {
                                const valid = isValid(currentBid, level, suit);
                                const chosen = isSameBid(selected, level, suit);
                                return (
                                    <td key={suit} style={{ padding: '2px' }}>
                                        <button
                                            disabled={!valid || !yourTurn}
                                            onClick={() => selectBid(level, suit)}
                                            style={{
                                                width: '48px',
                                                height: '32px',
                                                backgroundColor: chosen ? '#4f9dff' : valid ? '#eee' : '#f5f5f5',
                                                color: chosen ? 'white' : 'black',
                                                cursor: valid && yourTurn ? 'pointer' : 'not-allowed',
                                                opacity: valid ? 1 : 0.4,
                                            }}
                                        >
                                            {level}
                                            {SUIT_SYMBOLS[suit]}
                                        </button>
                                    </td>
                                );
                            })}
                        </tr>
                    ))}
                </tbody>
            </table>

            <div style={{ marginTop: '8px', display: 'flex', gap: '8px', alignItems: 'center' }}>
                <button
                    disabled={!yourTurn}
                    onClick={selectPass}
                    style={{
                        backgroundColor: passSelected ? '#4f9dff' : '#eee',
                        color: passSelected ? 'white' : 'black',
                    }}
                >
                    Pass
                </button>

                <button disabled={!yourTurn || !hasSelection} onClick={confirm}>
                    Confirm Bid
                </button>
            </div>
        </div>
    );
}