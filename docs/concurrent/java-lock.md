# Java 锁

> **📦 本文以及示例源码已归档在 [javacore](https://github.com/dunwu/javacore)**

<!-- TOC depthFrom:2 depthTo:3 -->

- [一、Java 锁简介](#一java-锁简介)
  - [锁分类](#锁分类)
  - [`synchronized` 和 `Lock`、`ReadWriteLock`](#synchronized-和-lockreadwritelock)
- [二、AQS](#二aqs)
  - [AQS 的要点](#aqs-的要点)
  - [AQS 的原理](#aqs-的原理)
- [三、Lock 接口](#三lock-接口)
  - [Lock 的要点](#lock-的要点)
  - [ReentrantLock 的用法](#reentrantlock-的用法)
  - [ReentrantLock 的原理](#reentrantlock-的原理)
- [四、ReadWriteLock 接口](#四readwritelock-接口)
  - [ReadWriteLock 的要点](#readwritelock-的要点)
  - [ReentrantReadWriteLock 的用法](#reentrantreadwritelock-的用法)
  - [ReentrantReadWriteLock 的原理](#reentrantreadwritelock-的原理)
- [参考资料](#参考资料)

<!-- /TOC -->

## 一、Java 锁简介

确保线程安全最常见的做法是利用锁机制（`Lock`、`sychronized`）来对共享数据做互斥同步，这样在同一个时刻，只有一个线程可以执行某个方法或者某个代码块，那么操作必然是原子性的，线程安全的。

### 锁分类

> :bulb: 基于不同维度，业界对于锁有多种分类。了解锁的分类，有助于我们理解锁的特性和设计原理。

#### 可重入锁

可重入锁又名递归锁，是指 **同一个线程在外层方法获取了锁，在进入内层方法会自动获取锁**。

- **`ReentrantLock` 是一个可重入锁**。这点，从其命名也不难看出。
- **`synchronized` 也是一个可重入锁**。

**可重入锁可以在一定程度上避免死锁**。

```java
synchronized void setA() throws Exception{
    Thread.sleep(1000);
    setB();
}

synchronized void setB() throws Exception{
    Thread.sleep(1000);
}
```

上面的代码就是一个可重入锁的一个特点，如果不是可重入锁的话，setB 可能不会被当前线程执行，可能造成死锁。

#### 公平锁与非公平锁

- **公平锁** - 公平锁是指 **多线程按照申请锁的顺序来获取锁**。
- **非公平锁** - 非公平锁是指 **多线程不按照申请锁的顺序来获取锁** 。有可能后申请的线程比先申请的线程优先获取锁。有可能，会造成优先级反转或者饥饿现象。

公平锁要保证线程申请顺序，势必要付出一定代价，自然效率上比非公平锁要低一些。

公平锁与非公平锁 在 Java 中的实现：

- **`synchronized` 是非公平锁**。
- Java 中的 **`ReentrantLock` ，默认是非公平锁，但可以在构造函数中指定该锁为公平锁**。

#### 独享锁与共享锁

- **独享锁** - 独享锁是指 **锁一次只能被一个线程所持有**。
- **共享锁** - 共享锁是指 **锁可被多个线程所持有**。

独享锁与共享锁在 Java 中的实现：

- **`synchronized` 是独享锁**。
- **`ReentrantLock` 是独享锁**。
- **`ReadWriteLock` 其读锁是共享锁，其写锁是独享锁**。读锁的共享锁可保证并发读是非常高效的，读写，写读 ，写写的过程是互斥的。

独享锁与共享锁是通过 `AQS` 来实现的，通过实现不同的方法，来实现独享或者共享。

#### 互斥锁与读写锁

上面讲的独享锁与共享锁就是一种广义的说法，互斥锁与读写锁就是具体的实现。

- **`synchronized` 是互斥锁**。
- **`ReentrantLock` 是互斥锁**。
- **`ReadWriteLock` 是读写锁**。

#### 悲观锁与乐观锁

乐观锁与悲观锁不是指具体的什么类型的锁，而是处理并发同步的策略。

- **悲观锁** - 悲观锁对于并发采取悲观的态度，认为：**不加锁的并发操作一定会出问题**。**悲观锁适合写操作频繁的场景**。
- **乐观锁** - 乐观锁对于并发采取乐观的态度，认为：不加锁的并发操作也没什么问题。对于同一个数据的并发操作，是不会发生修改的。在更新数据的时候，会采用不断尝试更新的方式更新数据。**乐观锁适合读多写少的场景**。

悲观锁与乐观锁在 Java 中的实现：

悲观锁在 Java 中的应用就是通过使用 `synchronized` 和 `Lock` 显示加锁来进行互斥同步，这是一种阻塞同步。

乐观锁在 Java 中的应用就是采用 CAS 机制（CAS 操作通过 `Unsafe` 类提供，但这个类不直接暴露为 API，所以都是间接使用。如各种原子类）。

#### 轻量级锁、重量级锁与偏向锁

所谓轻量级锁与重量级锁，指的是锁控制粒度的粗细。显然，控制粒度越细，阻塞开销越小，并发性也就越高。

Java 1.6 以前，重量级锁一般指的是 `synchronized` ，而轻量级锁指的是 `volatile`。

Java 1.6 以后，针对 `synchronized` 做了大量优化，引入 4 种锁状态： 无锁状态、偏向锁、轻量级锁和重量级锁。锁可以单向的从偏向锁升级到轻量级锁，再从升级的重量级锁 。

- **偏向锁** - 偏向锁是指一段同步代码一直被一个线程所访问，那么该线程会自动获取锁。降低获取锁的代价。
- **轻量级锁** - 是指当锁是偏向锁的时候，被另一个线程所访问，偏向锁就会升级为轻量级锁，其他线程会通过自旋的形式尝试获取锁，不会阻塞，提高性能。

- **重量级锁** - 是指当锁为轻量级锁的时候，另一个线程虽然是自旋，但自旋不会一直持续下去，当自旋一定次数的时候，还没有获取到锁，就会进入阻塞，该锁膨胀为重量级锁。重量级锁会让其他申请的线程进入阻塞，性能降低。

#### 分段锁

分段锁其实是一种锁的设计，并不是具体的一种锁。

例如：Java 1.7 以前，`ConcurrentHashMap` 通过分段锁设计，使得锁粒度更细，减少阻塞开销，从而提高并发性。

### `synchronized` 和 `Lock`、`ReadWriteLock`

在 [锁分类](#锁分类) 中，我们零零散散也提到了，`synchronized` 锁的限制比较多。在这里，汇总一下 `Lock` 、`ReadWriteLock` 相较于 `synchronized` 的优点：

- `synchronized` 获取锁和释放锁都是自动的，无法主动控制；`Lock` 可以手动获取锁、释放锁（但这也是一个定时炸弹，如果忘记释放锁，就可能产生死锁）。
- `synchronized` 不能响应中断；`Lock` 可以响应中断。
- `synchronized` 没有超时机制；`Lock` 可以设置超时时间，超时后自动释放锁，避免一直等待。
- `synchronized` 只能是非公平锁；`Lock` 可以选择公平锁或非公平锁两种模式。
- 被 `synchronized` 修饰的方法或代码块，只能被一个线程访问。如果这个线程被阻塞，其他线程也只能等待；`Lock` 可以基于 `Condition` 灵活的控制同步条件。
- `synchronized` 不支持读写锁分离；`ReadWriteLock` 支持读写锁，从而使阻塞读写的操作分开，有效提高并发性。

> 💡 `synchronized` 的用法和原理可以参考：[Java 并发基础机制 - synchronized](https://github.com/dunwu/javacore/blob/master/docs/concurrent/java-concurrent-basic-mechanism.md#%E4%BA%8Csynchronized) 。
>
> 如果不需要 `Lock` 、`ReadWriteLock` 所提供的高级同步特性，应该优先考虑使用 `synchronized` ，理由如下：
>
> - Java 1.6 以后，`synchronized` 做了大量的优化，其性能已经与 `Lock` 、`ReadWriteLock` 基本上持平。从趋势来看，Java 未来仍将继续优化 `synchronized` ，而不是 `ReentrantLock` 。
> - `ReentrantLock` 是 Oracle JDK 的 API，在其他版本的 JDK 中不一定支持；而 `synchronized` 是 JVM 的内置特性，所有 JDK 版本都提供支持。

## 二、AQS

> `AbstractQueuedSynchronizer`（队列同步器，简称 **AQS**）是 Java 的标准同步器。它是构建锁或者其他同步工具的实现基石（如 `ReentrantLock`、`ReentrantReadWriteLock`、`Semaphore` 等）。
>
> 因此，在深入理解 `ReentrantLock`、`ReentrantReadWriteLock` 前，应该先掌握 AQS 的原理。

### AQS 的要点

#### 什么是 AQS

在 LOCK 包中的相关锁(常用的有 ReentrantLock、 ReadWriteLock)都是基于 AQS 来构建。然而这些锁都没有直接来继承 AQS，而是定义了一个 Sync 类去继承 AQS。那么为什么要这样呢？因为锁面向的是使用用户，而同步器面向的则是线程控制，那么在锁的实现中聚合同步器而不是直接继承 AQS 就可以很好的隔离二者所关注的事情。

AQS 提供了对独享锁与共享锁的支持。

获取、释放独享锁 API

```java
public final void acquire(int arg)
public final void acquireInterruptibly(int arg)
public final boolean tryAcquireNanos(int arg, long nanosTimeout)
public final boolean release(int arg)
```

- `acquire` - 获取独占锁。
- `acquireInterruptibly` - 获取可中断的独占锁。
- `tryAcquireNanos` - 获取可中断的独占锁。在以下三种情况下回返回：
  - 在超时时间内，当前线程成功获取了锁；
  - 当前线程在超时时间内被中断；
  - 超时时间结束，仍未获得锁返回 false。
- `release` - 释放独占锁。

获取、释放共享锁 API

```java
public final void acquireShared(int arg)
public final void acquireSharedInterruptibly(int arg)
public final boolean tryAcquireSharedNanos(int arg, long nanosTimeout)
public final boolean releaseShared(int arg)
```

### AQS 的原理

#### 数据结构

阅读 AQS 的源码，可以发现：AQS 继承自 `AbstractOwnableSynchronize`。

```java
public abstract class AbstractQueuedSynchronizer
    extends AbstractOwnableSynchronizer
    implements java.io.Serializable {

    /** 等待队列的队头，懒加载。只能通过 setHead 方法修改。 */
    private transient volatile Node head;
    /** 等待队列的队尾，懒加载。只能通过 enq 方法添加新的等待节点。*/
    private transient volatile Node tail;
    /** 同步状态 */
    private volatile int state;
}
```

- `state` - AQS 在内部定义了一个 int 变量 state，用来**表示同步状态**。
  - 这个整数状态的意义由子类来赋予，如`ReentrantLock` 中该状态值表示所有者线程已经重复获取该锁的次数，`Semaphore` 中该状态值表示剩余的许可数量。
- `head` 和 `tail` - AQS **维护了一个 `Node` 类型（AQS 的内部类）的双链表来完成同步状态的管理**。这个双链表是一个双向的 FIFO 队列，通过 head 和 tail 指针进行访问。当 **有线程获取锁失败后，就被添加到队列末尾**。

![](http://dunwu.test.upcdn.net/cs/java/javacore/concurrent/aqs_1.png!zp)

再来看一下 `Node` 的源码

```java
static final class Node {
    /** 该等待同步的节点处于共享模式 */
    static final Node SHARED = new Node();
    /** 该等待同步的节点处于独占模式 */
    static final Node EXCLUSIVE = null;

    /** 等待状态,这个和 state 是不一样的:有 1,0,-1,-2,-3 五个值 */
    volatile int waitStatus;
    static final int CANCELLED =  1;
    static final int SIGNAL    = -1;
    static final int CONDITION = -2;
    static final int PROPAGATE = -3;

    /** 前驱节点 */
    volatile Node prev;
    /** 后继节点 */
    volatile Node next;
    /** 等待锁的线程 */
    volatile Thread thread;
}
```

很显然，Node 是一个双链表结构。重点关注一下 `volatile` 修饰的 `waitStatus` 属性，它用于维护 AQS 同步队列中线程节点的状态。`waitStatus` 有五个状态值：

- `CANCELLED(1)` - 此状态表示：该节点的线程可能由于超时或被中断而 **处于被取消(作废)状态**，一旦处于这个状态，表示这个节点应该从队列中移除。
- `SIGNAL(-1)` - 此状态表示：**后继节点会被挂起**，因此在当前节点释放锁或被取消之后，必须唤醒(`unparking`)其后继结点。
- `CONDITION(-2)` - 此状态表示：该节点的线程 **处于等待条件状态**，不会被当作是同步队列上的节点，直到被唤醒(`signal`)，设置其值为 0，再重新进入阻塞状态。
- `PROPAGATE(-3) -` - 此状态表示：下一个 `acquireShared` 应无条件传播。
- 0 - 非以上状态。

#### 独占锁的获取和释放

##### 获取独占锁

AQS 中使用 `acquire(int arg)` 方法获取独占锁，其大致流程如下：

1. 先尝试获取同步状态，如果获取同步状态成功，则结束方法，直接返回。
2. 如果获取同步状态不成功，AQS 会不断尝试利用 CAS 操作将当前线程插入等待同步队列的队尾，直到成功为止。
3. 接着，不断尝试为等待队列中的线程节点获取独占锁。

![](http://dunwu.test.upcdn.net/cs/java/javacore/concurrent/aqs_2.png!zp)

![](http://dunwu.test.upcdn.net/cs/java/javacore/concurrent/aqs_3.png!zp)

详细流程可以用下图来表示，请结合源码来理解（一图胜千言）：

![](http://dunwu.test.upcdn.net/cs/java/javacore/concurrent/aqs_4.png!zp)

##### 释放独占锁

AQS 中使用 `release(int arg)` 方法释放独占锁，其大致流程如下：

1. 先尝试获取解锁线程的同步状态，如果获取同步状态不成功，则结束方法，直接返回。
2. 如果获取同步状态成功，AQS 会尝试唤醒当前线程节点的后继节点。

##### 获取可中断的独占锁

AQS 中使用 `acquireInterruptibly(int arg)` 方法获取可中断的独占锁。

`acquireInterruptibly(int arg)` 实现方式**相较于获取独占锁方法（ `acquire`）非常相似**，区别仅在于它会**通过 `Thread.interrupted` 检测当前线程是否被中断**，如果是，则立即抛出中断异常（`InterruptedException`）。

##### 获取超时等待式的独占锁

AQS 中使用 `tryAcquireNanos(int arg)` 方法获取超时等待的独占锁。

doAcquireNanos 的实现方式 **相较于获取独占锁方法（ `acquire`）非常相似**，区别在于它会根据超时时间和当前时间计算出截止时间。在获取锁的流程中，会不断判断是否超时，如果超时，直接返回 false；如果没超时，则用 `LockSupport.parkNanos` 来阻塞当前线程。

#### 共享锁的获取和释放

##### 获取共享锁

AQS 中使用 `acquireShared(int arg)` 方法获取共享锁。

`acquireShared` 方法和 `acquire` 方法的逻辑很相似，区别仅在于自旋的条件以及节点出队的操作有所不同。

成功获得共享锁的条件如下：

- `tryAcquireShared(arg)` 返回值大于等于 0 （这意味着共享锁的 permit 还没有用完）。
- 当前节点的前驱节点是头结点。

##### 释放共享锁

AQS 中使用 `releaseShared(int arg)` 方法释放共享锁。

`releaseShared` 首先会尝试释放同步状态，如果成功，则解锁一个或多个后继线程节点。释放共享锁和释放独享锁流程大体相似，区别在于：

对于独享模式，如果需要 SIGNAL，释放仅相当于调用头节点的 `unparkSuccessor`。

##### 获取可中断的共享锁

AQS 中使用 `acquireSharedInterruptibly(int arg)` 方法获取可中断的共享锁。

`acquireSharedInterruptibly` 方法与 `acquireInterruptibly` 几乎一致，不再赘述。

##### 获取超时等待式的共享锁

AQS 中使用 `tryAcquireSharedNanos(int arg)` 方法获取超时等待式的共享锁。

`tryAcquireSharedNanos` 方法与 `tryAcquireNanos` 几乎一致，不再赘述。

## 三、Lock 接口

> 与内置锁 `synchronized` 不同，`Lock` 提供了一组无条件的、可轮询的、定时的以及可中断的锁操作，所有获取锁、释放锁操作都是显示的。

### Lock 的要点

`Lock` 的接口定义：

```java
public interface Lock {
    void lock();
    void lockInterruptibly() throws InterruptedException;
    boolean tryLock();
    boolean tryLock(long time, TimeUnit unit) throws InterruptedException;
    void unlock();
    Condition newCondition();
}
```

:bulb: 说明：

- `lock()` - 用于 **获取锁**。如果锁已被其他线程获取，则等待。
- `tryLock()` - 用于 **尝试获取锁，如果成功，则返回 true；如果失败，则返回 false**。也就是说，这个方法无论如何都会立即返回，获取不到锁（锁已被其他线程获取）时不会一直等待。
- `tryLock(long time, TimeUnit unit)` - 和 `tryLock()` 类似，区别仅在于这个方法在**获取不到锁时会等待一定的时间**，在时间期限之内如果还获取不到锁，就返回 false。如果如果一开始拿到锁或者在等待期间内拿到了锁，则返回 true。
- `lockInterruptibly()` - 当通过这个方法去获取锁时，如果线程正在等待获取锁，则这个线程能够响应中断，即**中断线程的等待状态**。也就使说，当两个线程同时通过 `lock.lockInterruptibly()` 想获取某个锁时，假若此时线程 A 获取到了锁，而线程 B 只有在等待，那么对线程 B 调用 `threadB.interrupt()` 方法能够中断线程 B 的等待过程。由于 `lockInterruptibly()` 的声明中抛出了异常，所以 `lock.lockInterruptibly()` 必须放在 try 块中或者在调用 `lockInterruptibly()` 的方法外声明抛出 `InterruptedException`。
- `unlock()` - 用于**释放锁**。

:bell: 注意：

- 如果采用 `Lock`，必须主动去释放锁，并且在发生异常时，不会自动释放锁。因此一般来说，使用 `Lock` 必须在 `try catch` 块中进行，并且将释放锁的操作放在 `finally` 块中进行，以保证锁一定被被释放，防止死锁的发生。
- 当一个线程获取了锁之后，是不会被 `interrupt()` 方法中断的。因为本身在前面的文章中讲过单独调用 `interrupt()` 方法不能中断正在运行过程中的线程，只能中断阻塞过程中的线程。因此当通过 `lockInterruptibly()` 方法获取某个锁时，如果不能获取到，只有进行等待的情况下，是可以响应中断的。

### ReentrantLock 的用法

`ReentrantLock` 实现了 `Lock` 接口，除了 `Lock` 接口所定义的能力，它还有以下特性：

- 提供了与 `synchronized` 相同的互斥性和内存可见性。
- 提供了与 `synchronized` 相同的可重入性。
- 支持公平锁和非公平锁（默认）两种模式。

#### 构造方法

```java
// 默认初始化 sync 的实例为非公平锁（NonfairSync）
public ReentrantLock() {}
// 根据 boolean 值选择初始化 sync 的实例为公平的锁（FairSync）或不公平锁（NonfairSync）
public ReentrantLock(boolean fair) {}
```

ReentrantLock 有两个构造方法：

- `ReentrantLock()` - 默认构造方法会初始化一个非公平锁；
- `ReentrantLock(boolean)` - `new ReentrantLock(true)` 会初始化一个公平锁。

```java
public class ReentrantLockDemo {

    public static void main(String[] args) {
        Task service = new Task();
        MyThread tA = new MyThread("Thread-A", service);
        MyThread tB = new MyThread("Thread-B", service);
        MyThread tC = new MyThread("Thread-C", service);
        tA.start();
        tB.start();
        tC.start();
    }

    static class MyThread extends Thread {

        private Task task;

        public MyThread(String name, Task task) {
            super(name);
            this.task = task;
        }

        @Override
        public void run() {
            super.run();
            task.execute();
        }

    }

    static class Task {

        private ReentrantLock lock = new ReentrantLock();

        public void execute() {
            lock.lock();
            try {
                for (int i = 0; i < 3; i++) {
                    System.out.println(Thread.currentThread().getName());

                    // 查询当前线程保持此锁的次数
                    System.out.println("\t holdCount: " + lock.getHoldCount());

                    // 返回正等待获取此锁的线程估计数
                    System.out.println("\t queuedLength: " + lock.getQueueLength());

                    // 如果此锁的公平设置为 true，则返回 true
                    System.out.println("\t isFair: " + lock.isFair());

                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } finally {
                lock.unlock();
            }
        }

    }

}
```

### ReentrantLock 的原理

`ReentrantLock` 维护了一个 `Sync` 对象，这是它实现 `Lock` 的关键。

`Sync` 是 `ReentrantLock` 的内部抽象类，它继承自 AQS。

`Sync` 有两个子类：

- `FairSync` - 公平锁。
- `NonfairSync` - 非公平锁。

## 四、ReadWriteLock 接口

### ReadWriteLock 的要点

`ReadWriteLock` 接口定义如下：

```java
public interface ReadWriteLock {
    Lock readLock();
    Lock writeLock();
}
```

- `readLock` - 返回用于读操作的锁。
- `writeLock` - 返回用于写操作的锁。

### ReentrantReadWriteLock 的用法

`ReentrantReadWriteLock` 类是 `ReadWriteLock` 的具体实现。它是一个**可重入的读写锁**。

```java
public class ReentrantReadWriteLockDemo {

    private ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();

    public static void main(String[] args) {
        final ReentrantReadWriteLockDemo demo = new ReentrantReadWriteLockDemo();
        new Thread(() -> demo.get(Thread.currentThread())).start();
        new Thread(() -> demo.get(Thread.currentThread())).start();
    }

    public synchronized void get(Thread thread) {
        rwl.readLock().lock();
        try {
            long start = System.currentTimeMillis();

            while (System.currentTimeMillis() - start <= 1) {
                System.out.println(thread.getName() + "正在进行读操作");
            }
            System.out.println(thread.getName() + "读操作完毕");
        } finally {
            rwl.readLock().unlock();
        }
    }
}
```

### ReentrantReadWriteLock 的原理

对于特定的资源，ReadWriteLock 允许多个线程同时对其执行读操作，但是只允许一个线程对其执行写操作。

ReadWriteLock 维护一对相关的锁。一个是读锁；一个是写锁。将读写锁分开，有利于提高并发效率。

ReentrantReadWriteLock 实现了 ReadWriteLock 接口，所以它是一个读写锁。

“读-读”线程之间不存在互斥关系。

“读-写”线程、“写-写”线程之间存在互斥关系。

<p align="center">
  <img src="http://dunwu.test.upcdn.net/cs/java/javacore/concurrent/ReadWriteLock.jpg">
</p>

## 参考资料

- [《Java 并发编程实战》](https://item.jd.com/10922250.html)
- [《Java 并发编程的艺术》](https://item.jd.com/11740734.html)
- [Java 并发编程：Lock](https://www.cnblogs.com/dolphin0520/p/3923167.html)
- [深入学习 java 同步器 AQS](https://zhuanlan.zhihu.com/p/27134110)
- [AbstractQueuedSynchronizer 框架](https://t.hao0.me/java/2016/04/01/aqs.html)
- [Java 中的锁分类](https://www.cnblogs.com/qifengshi/p/6831055.html)
