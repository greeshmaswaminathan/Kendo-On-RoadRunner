package test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public class ParallelQuickSort {

	private final ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
	private AtomicInteger counter = new AtomicInteger(0);
	
	public void sort(int[] values) throws InterruptedException, ExecutionException {
		Future<?> task = pool.submit(new QuickSortThread(values, 0, values.length - 1, counter.incrementAndGet()));
		task.get();
	}

	private int partition(int[] values, int low, int high) {
		int pivot = values[high];
		int swapIndex = low; // place for swapping
		for (int index = low; index < high; index++) {
			if (values[index] <= pivot) {
				swap(values, swapIndex, index);
				swapIndex++;
			}
		}
		swap(values, swapIndex, high);
 
		return swapIndex;
	}

	private void swap(int[] values, int swapIndex, int index) {
		int temp = values[swapIndex];
		values[swapIndex] = values[index];
		values[index] = temp;
	}

	public static void main(String[] args) throws InterruptedException, ExecutionException {
		int[] values = { 1, 2, 5, 8, 3, 11, 3, 7 };
		new ParallelQuickSort().sort(values);
		for (int index = 0; index < values.length; index++) {
			System.out.print(values[index] + " ");
		}
	}

	class QuickSortThread implements Runnable {

		private int[] values;
		private int low = 0;
		private int high = 0;
		private int identifier = 0;

		public QuickSortThread(int[] values, int low, int high, int counter) {
			this.values = values;
			this.high = high;
			this.low = low;
			this.identifier = counter;
		}

		@Override
		public void run() {
			System.out.println("In run of thread with identifier "+identifier);
			try {
				if (low < high) {
					int pivotIndex = partition(values, low, high);
					Future<?> left = pool.submit(new QuickSortThread(values, low, pivotIndex - 1, counter.incrementAndGet()));
					Future<?> right = pool.submit(new QuickSortThread(values, pivotIndex + 1, high, counter.incrementAndGet()));
					left.get();
					right.get();
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

	}

}
