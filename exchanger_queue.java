// Qingchuan Ma
// 260740719

import java.io.*;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.concurrent.atomic.*;
import java.util.Comparator;

class Data{
	public String data;
	public int priority;
	public Data next;
	public Data (int i, char c, int p){
		priority = p;
		data = "" + i + c;
		next = null;
	}
}

class Output{
	long time;
	String data;
	int priority;
	int id;
	String operation;
	public Output(String d, int p, int i, int o){
		time = System.nanoTime();
		data = d;
		priority = p;
		id = i;
		if (o == 0){
			operation = "add";
		}
		else {
			operation = "del";
		}
	}
	@Override
	public String toString(){
		if (data.equals("*")){
			return time + " " + id + " " + operation + " " + data;
		}
		return time + " " + id + " " + operation + " " + data + " " + priority;
	}
}

class Stack{
	AtomicReference<Data> top;
	Exchanger ex;
	public Stack(int p){
		top = new AtomicReference<Data>();
		ex = new Exchanger(p);
	}

	public void push(Data v){
		Data n = v;
		Data o;
		do {
			o = top.get();
			n.next = o;
		}
		while (!top.compareAndSet(o, n));
	}

	public Data pop(){
		Data o;
		Data n;
		do {
			o = top.get();
			if (o == null){
				return null;
			}
			n = o.next;
		}
		while (!top.compareAndSet(o, n));
		o.next = null;
		return o;
	}
}

class PriorityQueue{
	ArrayList<Stack> queue;
	public PriorityQueue(){
		queue = new ArrayList<Stack>();
		for (int i = 0; i < 10; i++){
			queue.add(new Stack(i));
		}
	}
	public void add(Data v){
		queue.get(v.priority).push(v);
	}
	public Data deleteMin(){
		for (int i = 0; i < queue.size(); i++){
			Stack s = queue.get(i);
			if (s == null){
				return null;
			}
			Data result = s.pop();
			if(result != null){
				return result;
			}
		}
		return null;
	}
}

class Exchanger{
	static final int EMPTY = 0;
	static final int WAITING = 1;
	static final int BUSY = 2;
	AtomicStampedReference<String> slot;
	int priority;
	public Exchanger(int p){
		slot = new AtomicStampedReference<>(null,0);
		priority = p;
	}
	public boolean exchange (Data d, long t, TimeUnit u, DataThread th){
		long nanos = u.toNanos(t);
		long bound = System.nanoTime() + nanos;
		int[] stampHolder = new int[1];
		stampHolder[0] = 0;
		while(true){
			if (System.nanoTime() > bound){
				break;
			}
			String other = slot.get(stampHolder);
			int stamp = stampHolder[0];
			switch(stamp){
				case 0:
					if (d == null){
						return false;
					}
					String temp = d.data;
					if(slot.compareAndSet(other, d.data, EMPTY, WAITING)){
						while(System.nanoTime() < bound){
							other = slot.get(stampHolder);
							if(stampHolder[0] == BUSY){
								slot.set(null, EMPTY);
								th.outputs.add(new Output(temp, priority, th.id, 0));
								return true;
							}
						}
						if(slot.compareAndSet(d.data, null, WAITING, EMPTY)){
							break;
						}
						else{
							other = slot.get(stampHolder);
							slot.set(null, EMPTY);
							th.outputs.add(new Output(temp, priority, th.id, 0));
							return true;
						}
					}
					break;
				case 1:
					if (d != null){
						return false;
					}
					String otherdata = other;
					if(slot.compareAndSet(other, null, WAITING, BUSY)){
						th.outputs.add(new Output(otherdata, priority, th.id, 1));
						return true;
					}
					break;
				case 2:
					return false;
				default:
					return false;
			}
		}
		return false;
	}
}

class DataThread extends Thread{
	int id;
	ArrayList<Data> deletes;
	ArrayList<Output> outputs;
	public DataThread(int i){
		id = i;
		deletes = new ArrayList<Data>();
		outputs = new ArrayList<Output>();
	}
	public void add(){
		char c = (char)Math.round(65+(90-65)*Math.random());
		int p = (int)(Math.random()*10);
		if (deletes.size() != 0){
			Data temp = deletes.get(0);
			temp.data = ""+id+c;
			temp.priority = p;
			if (!q2.queue.queue.get(temp.priority).ex.exchange(temp, q2.timeout, q2.unit, this)){
				q2.queue.add(temp);
				outputs.add(new Output(temp.data, temp.priority, id, 0));
			}
			deletes.remove(0);
		}
		else {
			Data temp = new Data(id, c, p);
			if (!q2.queue.queue.get(temp.priority).ex.exchange(temp, q2.timeout, q2.unit, this)){
				q2.queue.add(temp);
				outputs.add(new Output(temp.data, temp.priority, id, 0));
			}
		}
	}
	public void delete(){
		Data temp = null;
		int pos = -1;
		boolean exchanged = false;
		for (int i = 0; i < q2.queue.queue.size(); i++){
			if (q2.queue.queue.get(i).top != null){
				pos = i;
			}
		}
		if (pos != -1){
			exchanged = q2.queue.queue.get(pos).ex.exchange(temp, q2.timeout, q2.unit, this);
		}
		if (!exchanged){
			temp = q2.queue.deleteMin();
			if (temp == null){
				outputs.add(new Output("*", -1, id, 1));
			}
			else {
				outputs.add(new Output(temp.data, temp.priority, id, 1));
				deletes.add(temp);
				if (deletes.size() > 10){
					deletes.remove(0);
				}
			}
		}
	}
	@Override
	synchronized public void run(){
		for (int i = 0; i < q2.n; i++){
			double temp = Math.random();
			if (temp < q2.q){
				add();
			}
			else {
				delete();
			}
			try {
				long stime = Math.round(q2.d*Math.random());
				this.sleep(stime);
			}
			catch (InterruptedException e){
				e.printStackTrace();
			}
		}
	}
}

public class exchanger_queue{
	public static PriorityQueue queue;
	public static TimeUnit unit;
	public static long timeout;
	public static int t;
	public static double q;
	public static int d;
	public static int n;
	public static void main(String[] args){
		try {
			queue = new PriorityQueue();
			unit = TimeUnit.MILLISECONDS;
			timeout = 1;
			t = Integer.parseInt(args[0]);
			q = Double.parseDouble(args[1]);
			d = Integer.parseInt(args[2]);
			n = Integer.parseInt(args[3]);
			ArrayList<DataThread> threads = new ArrayList<DataThread>();
			for(int i = 0; i < t; i++){
				double r = Math.random();
				DataThread temp = new DataThread(i);
				threads.add(temp);
			}
			long start = System.currentTimeMillis();
			for (int i = 0; i < t; i++){
				threads.get(i).start();
			}
			for (int i = 0; i < t; i++){
				threads.get(i).join();
			}
			long end = System.currentTimeMillis();
			ArrayList<Output> overall = new ArrayList<Output>();
			for (int i = 0; i < t; i++){
				overall.addAll(threads.get(i).outputs);
			}
			overall.sort(Comparator.comparing((Output o)->o.time));
			int ite = overall.size();
			if (overall.size() > 1000){
				ite = 1000;
			}
			for (int i = 0; i < ite; i++){
				// System.out.println(overall.get(i).toString());
			}
			System.out.println("The time spent: " + (end - start));
		}
		catch (Exception e){
			System.out.println("ERROR " +e);
			e.printStackTrace();
		}
	}
}