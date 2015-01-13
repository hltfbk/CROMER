package servlet.viewer;

/*************************************************************************
 *  Compilation:  javac Queue.java
 *  Execution:    java Queue
 *
 *  A generic queue, implemented using a linked list. Each queue
 *  element is of type Item.
 *
 *************************************************************************/

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

public class Queue implements Iterable {
    private int N;         // number of elements on queue
    private Node first;    // beginning of queue
    private Node last;     // end of queue

    // helper linked list class
    private class Node {
        private Object item;
        private Node next;

        Node(Object item, Node next) {
            this.item = item;
            this.next = next;
        }
    }

    // is the queue empty?
    public boolean isEmpty() { return first == null; }
    public int length()      { return N;             }
    public int size()        { return N;             }

    // add an item to the queue
    public void enqueue(Object item) {
        Node x = new Node(item, null);
        if (isEmpty()) { first = x;     last = x; }
        else           { last.next = x; last = x; }
        N++;
    }

    // remove and return the least recently added item
    public Object dequeue() {
        if (isEmpty()) throw new RuntimeException("Queue underflow");
        Object item = first.item;
        first = first.next;
        N--;
        return item;
    }

    // remove all elements
    public void clear () {
        while (!isEmpty()) {
            dequeue();
        }
    }

    // remove and return the least recently added item
    public String toString() {
        String s = "";
        for (Node x = first; x != null; x = x.next)
            s += x.item;
        return s;
    }

    public Iterator iterator()  { return new QueueIterator();  }

    // an iterator, doesn't implement remove() since it's optional
    private class QueueIterator implements Iterator {
        private Node current = first;

        public boolean hasNext()  { return current != null;                     }
        public void remove()      { throw new UnsupportedOperationException();  }

        public Object next() {
            if (!hasNext()) throw new NoSuchElementException();
            Object item = current.item;
            current = current.next;
            return item;
        }
    }



    // a test client
    public static void main(String[] args) {
        String line = "#In#in##the#the##prior#prior#a#art#art#n#,###cutting#cutting#n#or#or##photovaporization##n#occurs#occur#v#when#when##light#light#a";
        System.err.println("Pattern " + Pattern.matches(".*cutting#.*#light#.*$", line) + "\n");

        /***********************************************
         *  A queue of strings
         ***********************************************/
        Queue q1 = new Queue();
        q1.enqueue("Vertigo");
        q1.enqueue("Just Lose It");
        q1.enqueue("Pieces of Me");
        System.out.println(q1.dequeue());
        q1.enqueue("Drop It Like It's Hot");
        while (!q1.isEmpty())
            System.out.println(q1.dequeue());
        System.out.println();


        /*********************************************************
         *  A queue of integers. Illustrates autoboxing and
         *  auto-unboxing.
         *********************************************************/
        Queue q2 = new Queue();
        for (int i = 0; i < 10; i++)
            q2.enqueue(new Integer(i));

        // test out iterator
        for (int i = 0; i < 10; i++)
            System.err.print(q2.dequeue() + " ");
        System.err.println();

        // test out dequeue and enqueue
        while (q2.size() >= 2) {
            Integer a = (Integer)q2.dequeue();
            Integer b = (Integer)q2.dequeue();
            System.err.println(a + " " +b);
        }

    }
}



