package javaProg;

import java.util.*;

public class DiscountProducts {

	public static void main(String[] args) {
	
		List<Integer> price = Arrays.asList(10, 20, 30, 40, 50, 50, 60);
		calculateDiscount(price);
	}

	public static void calculateDiscount(List<Integer> prices) {
		List<Integer> sortedPrice = new ArrayList<>(prices);
		Collections.sort(sortedPrice, Collections.reverseOrder());

		List<Integer> payItems = new ArrayList<>();
		List<Integer> disItems = new ArrayList<>();

		int i = 0;

		while (i < sortedPrice.size()) {
			if (i + 1 < sortedPrice.size()) {
				payItems.add(sortedPrice.get(i));
				disItems.add(sortedPrice.get(i + 1));
				i += 2;
			} else {
				payItems.add(sortedPrice.get(i));
				i++;
			}
		}
		System.out.println("Discounted Items (Free): " + disItems);
		System.out.println("Payable Items: " + payItems);
	}
}


