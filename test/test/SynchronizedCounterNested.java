package test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class SynchronizedCounterNested implements Runnable{
	private int counter = 0;
	//private Lock lock1 = new ReentrantLock();
	//private Lock lock2 = new ReentrantLock();
	private Object randomObj = new Object();
	
	static List<String> threadOrder = new ArrayList<>();
	private static CountDownLatch startSignal = new CountDownLatch(1);
	
	public SynchronizedCounterNested() {
		
	}
	public synchronized void increment() throws InterruptedException {
		
		//threadOrder.add(Thread.currentThread().getName()+" ");
		counter++;
		
		//simulating deadlock
		
		decrement();
		
		
	}

	public void decrement() throws InterruptedException {
		synchronized(randomObj){
			threadOrder.add(Thread.currentThread().getName()+" ");
			counter--;
			
		}
		
	}

	public synchronized int value() {
		return counter;
	}
	
	public static void main(String[] args) throws InterruptedException {
		long startTime = System.currentTimeMillis();
		SynchronizedCounterNested counter = new SynchronizedCounterNested();
		Thread t1 = createThreads(counter,1);
		Thread t2 = createThreads(counter,2);
		Thread t3 = createThreads(counter,3);
		Thread t4 = createThreads(counter,4);
		Thread t5 = createThreads(counter,5);
		t1.start();
		t2.start();
		t3.start();
		t4.start();
		t5.start();
		startSignal.countDown(); 
		//Thread.sleep(2000);
		t1.join();
		t2.join();
		t3.join();
		t4.join();
		t5.join();
		for (String string : threadOrder) {
			System.out.print(string+" ");
		}
		System.out.println("");
		System.out.println("Completed in "+(System.currentTimeMillis() - startTime));
		//
		//
	}
	
	private static Thread createThreads(SynchronizedCounterNested counter, int threadId){
		return new Thread(counter,threadId+"");
	}

	@Override
	public void run() {
		try {
			startSignal.await();
			
			for(int i = 0; i< 5; i++){
				increment();
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}

