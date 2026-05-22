package game.manager.table.ddz.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import game.manager.table.cards.Card;

/**
 * Combinations
 * 组合
 * 
 * @author cloud
 * @date 2026-05-03
 * @version 1.0
 * @since 1.0
 */
final class Combinations {

	private Combinations() {
	}

	/**
	 * 遍历组合
	 * 
	 * @param source   源数据
	 * @param k        组合数量
	 * @param consumer 消费者
	 */
	static void forEachCombination(List<Card> source, int k, Consumer<List<Card>> consumer) {
		if (k <= 0 || k > source.size()) {
			return;
		}
		int[] idx = new int[k];
		for (int i = 0; i < k; i++) {
			idx[i] = i;
		}
		while (true) {
			List<Card> pick = new ArrayList<>(k);
			for (int i : idx) {
				pick.add(source.get(i));
			}
			consumer.accept(pick);
			int t = k - 1;
			while (t >= 0 && idx[t] == source.size() - k + t) {
				t--;
			}
			if (t < 0) {
				break;
			}
			idx[t]++;
			for (int i = t + 1; i < k; i++) {
				idx[i] = idx[i - 1] + 1;
			}
		}
	}
}
