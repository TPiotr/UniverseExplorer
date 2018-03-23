package explorer.game.framework.utils;

import com.esotericsoftware.minlog.Log;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadPool {
	
	//create executor rejecting handler
	class TerRejectedExecutionHandler implements RejectedExecutionHandler {
		
		//for now don't do anything
		public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
			Log.error("Not enough worker threads, running task on main thread!");
			r.run();
		}
	}
	
	//class used to add id to threads
	class ThreadIDAssigner {
		private AtomicInteger next_id = new AtomicInteger();
		public synchronized int next() {
			return next_id.getAndIncrement();
		}
	}
	
	//out executor
	private ThreadPoolExecutor executor_pool;
	
	//params
	private int thread_pool_size = 15;
	private long keep_alive = 1;
	private int threads_count; 
	
	public ThreadPool() {
		threads_count = Runtime.getRuntime().availableProcessors();
		//threads_count = 1;

		threads_count = (threads_count - 1 <= 0) ? 1 : threads_count - 1;
		Log.info("Aval. processors for app: " + threads_count);

		//create executor
		TerRejectedExecutionHandler rejection_handler = new TerRejectedExecutionHandler();
        
		//Get the ThreadFactory implementation to use
		final ThreadIDAssigner id_assigner = new ThreadIDAssigner();
		ThreadFactory thread_factory = new ThreadFactory() {
			public Thread newThread(Runnable r) {
				Thread thread = new Thread(r, "Universe-Explorer-WorkerThread-" + id_assigner.next());
				thread.setPriority(Thread.MIN_PRIORITY);
				
				//use daemon true because this thread will be terminated only when main application thread will be killed
				thread.setDaemon(true);
				return thread;
			}
		};

        //creating the ThreadPoolExecutor
        //thread_pool_size means max pool space
        //thread_num means how many threads will work at the moment
        //keep_alive means how to keep alive
        //thread_num-1 because we already use one thread as out main rendering and logic gdx thread
        executor_pool = new ThreadPoolExecutor(threads_count, thread_pool_size, keep_alive,
       		TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(threads_count), thread_factory, rejection_handler);
	}
	
	public synchronized void runTask(Runnable runnable) {
		executor_pool.execute(runnable);
	}

	public Future<?> runTaskFuture(Runnable runnable) {
		return executor_pool.submit(runnable);
	}

	/**
	 * func that returns how many tasks are currently running
	 */
	public int getActuallyTasksRunningCount() {
		return (int) (executor_pool.getTaskCount() - executor_pool.getCompletedTaskCount());
	}
	
	public void dispose() {
		executor_pool.shutdown();
	}
}
