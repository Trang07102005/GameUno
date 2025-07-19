package com.example.gameuno;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UnoDeck {

    private final List<UnoCard> cards;

    public UnoDeck() {
        cards = new ArrayList<>();
        initializeDeck();
        shuffle();
    }

    /**
     * Tạo 108 lá bài đầy đủ theo quy tắc PascalCase.
     */
    private void initializeDeck() {
        cards.clear(); // Đảm bảo danh sách rỗng trước khi khởi tạo
        UnoCard.Color[] colors = {
                UnoCard.Color.Red,
                UnoCard.Color.Yellow,
                UnoCard.Color.Green,
                UnoCard.Color.Blue
        };

        // Các lá thông thường: mỗi màu có
        for (UnoCard.Color color : colors) {
            // 1 lá Zero
            cards.add(new UnoCard(color, UnoCard.Value.Zero));

            // 2 lá mỗi số từ 1 đến 9 + Skip, Reverse, DrawTwo
            UnoCard.Value[] values = {
                    UnoCard.Value.One, UnoCard.Value.Two, UnoCard.Value.Three, UnoCard.Value.Four,
                    UnoCard.Value.Five, UnoCard.Value.Six, UnoCard.Value.Seven, UnoCard.Value.Eight,
                    UnoCard.Value.Nine, UnoCard.Value.Skip, UnoCard.Value.Reverse, UnoCard.Value.DrawTwo
            };

            for (UnoCard.Value value : values) {
                // Mỗi lá 2 bản
                cards.add(new UnoCard(color, value));
                cards.add(new UnoCard(color, value));
            }
        }

        // Các lá Wild: 4 Wild, 4 WildDrawFour
        for (int i = 0; i < 4; i++) {
            cards.add(new UnoCard(UnoCard.Color.Wild, UnoCard.Value.Wild));
            cards.add(new UnoCard(UnoCard.Color.Wild, UnoCard.Value.WildDrawFour));
        }
    }

    /**
     * Trộn bài.
     */
    public void shuffle() {
        Collections.shuffle(cards);
    }

    /**
     * Kiểm tra xem bộ bài có rỗng không.
     */
    public boolean isEmpty() {
        return cards.isEmpty();
    }

    /**
     * Rút 1 lá từ bộ bài, tái tạo bộ bài nếu rỗng.
     */
    public UnoCard drawCard() {
        if (isEmpty()) {
            initializeDeck();
            shuffle();
        }
        return cards.remove(0);
    }

    /**
     * Xem còn bao nhiêu lá.
     */
    public int remainingCards() {
        return cards.size();
    }
}