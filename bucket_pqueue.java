// Qingchuan Ma
// 260740719

import java.io.*;
import java.util.concurrent.locks.*;
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
	ReentrantLock lock;
	public Stack(){
		lock = new ReentrantLock();
		top = new AtomicReference<Data>();
	}
	public void push(Data v){
		lock.lock();
		Data o;
		Data n;
		try{
			n = v;
			o = top.get();
			n.next = o;
			top.set(n);
		}
		finally{
			lock.unlock();
		}
	}
	public Data pop(){
		lock.lock();
		Data o;
		Data n;
		try{
			o = top.get();
			if (o == null){
				return null;
			}
			n = o.next;
			top.set(n);
			o.next = null;
			return o;
		}
		finally{
			lock.unlock();
		}
	}
}

class PriorityQueue{
	ArrayList<Stack> queue;
	public PriorityQueue(){
		queue = new ArrayList<Stack>();
		for (int i = 0; i < 10; i++){
			queue.add(new Stack());
		}
	}
	public void add(Data v){
		queue.get(v.priority).push(v);
	}
	public Data deleteMin(){
		for (int i = 0; i < queue.size(); i++){
			Data result = queue.get(i).pop();
			if(result != null){
				return result;
			}
		}
		return null;
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
			q1.queue.add(temp);
			deletes.remove(0);
			outputs.add(new Output(temp.data, temp.priority, id, 0));
		}
		else {
			Data temp = new Data(id, c, p);
			q1.queue.add(temp);
			outputs.add(new Output(temp.data, temp.priority, id, 0));
		}
	}
	public void delete(){
		Data temp = q1.queue.deleteMin();
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
	@Override
	synchronized public void run(){
		for (int i = 0; i < q1.n; i++){
			double temp = Math.random();
			if (temp < q1.q){
				add();
			}
			else {
				delete();
			}
			try {
				long stime = Math.round(q1.d*Math.random());
				this.sleep(stime);
			}
			catch (InterruptedException e){
				e.printStackTrace();
			}
		}
	}
}

public class bucket_pqueue{
	public static PriorityQueue queue;
	public static int t;
	public static double q;
	public static int d;
	public static int n;
	public static void main(String[] args){
		try {
			queue = new PriorityQueue();
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
