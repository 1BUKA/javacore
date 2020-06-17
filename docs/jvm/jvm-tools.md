# JVM 工具

> Java 程序员免不了故障排查工作，所以经常需要使用一些 JVM 工具。

<!-- TOC depthFrom:2 depthTo:3 -->

- [一、JVM CLI 工具](#一jvm-cli-工具)
  - [jps](#jps)
  - [jstat](#jstat)
  - [jmap](#jmap)
  - [jstack](#jstack)
  - [jhat](#jhat)
  - [jinfo](#jinfo)
- [二、JVM GUI 工具](#二jvm-gui-工具)
  - [jconsole](#jconsole)
  - [jvisualvm](#jvisualvm)
  - [MAT](#mat)
- [参考资料](#参考资料)

<!-- /TOC -->

## 一、JVM CLI 工具

JDK 自带了一些实用的命令行工具来监控 JVM。

| 名称     | 描述                                                                                                 |
| -------- | ---------------------------------------------------------------------------------------------------- |
| `jps`    | 显示指定系统内所有的 HotSpot 虚拟机进程。                                                            |
| `jstat`  | 用于监视虚拟机运行时状态信息，它可以显示出虚拟机进程中的类装载、内存、垃圾收集、JIT 编译等运行数据。 |
| `jmap`   | 用于生成堆转储快照（一般称为 heapdump 或 dump 文件）。                                               |
| `jstack` | 用于生成 java 虚拟机当前时刻的线程快照（一般称为 threaddump 或 javacore 文件）。                     |
| `jhat`   | 用来分析 jmap 生成的 dump 文件。                                                                     |
| `jinfo`  | 用于实时查看和调整虚拟机运行参数。                                                                   |

### jps

> **[jps(JVM Process Status Tool)](https://docs.oracle.com/en/java/javase/11/tools/jps.html#GUID-6EB65B96-F9DD-4356-B825-6146E9EEC81E) 是虚拟机进程状态工具**。它可以显示指定系统内所有的 HotSpot 虚拟机进程状态信息。jps 通过 RMI 协议查询开启了 RMI 服务的远程虚拟机进程状态。

命令格式：

```shell
jps [option] [hostid]
```

如果不指定 hostid 就默认为当前主机或服务器。

常用参数：

- `option` - 选项参数
  - `-l` - 输出主类的全名，如果进程执行的是 jar 包，输出 jar 路径
  - `-m` - 输出 JVM 启动时传递给 main() 的参数
  - `-q` - 只输出 LVMID，省略主类的名称
  - `-v` - 输出 JVM 启动时显示指定的 JVM 参数
- `hostid` - RMI 注册表中注册的主机名。如果不指定 hostid 就默认为当前主机或服务器。

其中[option]、[hostid]参数也可以不写。

【示例】

```shell
$ jps -l -m
28920 org.apache.catalina.startup.Bootstrap start
11589 org.apache.catalina.startup.Bootstrap start
25816 sun.tools.jps.Jps -l -m
```

### jstat

> **[jstat(JVM statistics Monitoring)](https://docs.oracle.com/en/java/javase/11/tools/jstat.html)，是虚拟机统计信息监视工具**。jstat 用于监视虚拟机运行时状态信息，它可以显示出虚拟机进程中的类装载、内存、垃圾收集、JIT 编译等运行数据。

命令格式：

```shell
jstat [option] VMID [interval] [count]
```

常用参数：

- `option` - 选项参数，用于指定用户需要查询的虚拟机信息
  - `-class` - 监视类装载、卸载数量、总空间以及类装载所耗费的时间
  - `-gc` - 监视 Java 堆状况，包括 Eden 区、两个 survivor 区、老年代、永久代等区的容量、已用空间、GC 时间合计等信息。
- `VMID` - 如果是本地虚拟机进程，则 VMID 与 LVMID 是一致的；如果是远程虚拟机进程，那 VMID 的格式应当是：`[protocol:][//]lvmid[@hostname[:port]/servername]`
- `interval` - 查询间隔
- `count` - 查询次数

> 【参考】更详细说明可以参考：[jstat 命令查看 jvm 的 GC 情况](https://www.cnblogs.com/yjd_hycf_space/p/7755633.html)

#### 类加载统计

使用 `jstat -class pid` 命令可以查看编译统计信息。

【参数】

- Loaded - 加载 class 的数量
- Bytes - 所占用空间大小
- Unloaded - 未加载数量
- Bytes - 未加载占用空间
- Time - 时间

【示例】查看类加载信息

```shell
$ jstat -class 7129
Loaded  Bytes  Unloaded  Bytes     Time
 26749 50405.3      873  1216.8      19.75
```

【示例】每秒打印 1 次 GC 信息，打印 4 次

```shell
$ jstat -gc 25196 1s 4
 S0C    S1C    S0U    S1U      EC       EU        OC         OU       MC     MU    CCSC   CCSU   YGC     YGCT    FGC    FGCT     GCT   
20928.0 20928.0  0.0    0.0   167936.0  8880.5   838912.0   80291.2   106668.0 100032.1 12772.0 11602.2    760   14.332  580   656.218  670.550
20928.0 20928.0  0.0    0.0   167936.0  8880.5   838912.0   80291.2   106668.0 100032.1 12772.0 11602.2    760   14.332  580   656.218  670.550
20928.0 20928.0  0.0    0.0   167936.0  8880.5   838912.0   80291.2   106668.0 100032.1 12772.0 11602.2    760   14.332  580   656.218  670.550
20928.0 20928.0  0.0    0.0   167936.0  8880.5   838912.0   80291.2   106668.0 100032.1 12772.0 11602.2    760   14.332  580   656.218  670.550
```

#### 编译统计

使用 `jstat -compiler pid` 命令可以查看编译统计信息。

【示例】

```shell
$ jstat -compiler 7129
Compiled Failed Invalid   Time   FailedType FailedMethod
   42030      2       0   302.53          1 org/apache/felix/framework/BundleWiringImpl$BundleClassLoader findClass
```

【参数】

- Compiled - 编译数量
- Failed - 失败数量
- Invalid - 不可用数量
- Time - 时间
- FailedType - 失败类型
- FailedMethod - 失败的方法

#### GC 统计

使用 `jstat -gc pid time` 命令可以查看 GC 统计信息。

【示例】

```shell
$ jstat -gc 29527 200 5
 S0C    S1C    S0U    S1U      EC       EU        OC         OU       MC     MU    CCSC   CCSU   YGC     YGCT    FGC    FGCT     GCT
22528.0 22016.0  0.0   21388.2 4106752.0 921244.7 5592576.0  2086826.5  110716.0 103441.1 12416.0 11167.7   3189   90.057  10      2.140   92.197
22528.0 22016.0  0.0   21388.2 4106752.0 921244.7 5592576.0  2086826.5  110716.0 103441.1 12416.0 11167.7   3189   90.057  10      2.140   92.197
22528.0 22016.0  0.0   21388.2 4106752.0 921244.7 5592576.0  2086826.5  110716.0 103441.1 12416.0 11167.7   3189   90.057  10      2.140   92.197
22528.0 22016.0  0.0   21388.2 4106752.0 921244.7 5592576.0  2086826.5  110716.0 103441.1 12416.0 11167.7   3189   90.057  10      2.140   92.197
22528.0 22016.0  0.0   21388.2 4106752.0 921244.7 5592576.0  2086826.5  110716.0 103441.1 12416.0 11167.7   3189   90.057  10      2.140   92.197
```

【参数】

- `S0C` - 第一个 Survivor 区的大小
- `S1C` - 第二个 Survivor 区的大小
- `S0U` - 第一个 Survivor 区的使用大小
- `S1U` - 第二个 Survivor 区的使用大小
- `EC` - Eden 区的大小
- `EU` - Eden 区的使用大小
- `TT` - 对象在新生代存活的次数
- `MTT` - 对象在新生代存活的最大次数
- `DSS` - 期望的 Survivor 区的大小
- `YGC` - 年轻代垃圾回收次数
- `YGCT` - 年轻代垃圾回收消耗时间

### jmap

> **[jmap(JVM Memory Map)](https://docs.oracle.com/en/java/javase/11/tools/jmap.html) 是 Java 内存映像工具**。jmap 用于生成堆转储快照（一般称为 heapdump 或 dump 文件）。jmap 不仅能生成 dump 文件，还可以查询 `finalize` 执行队列、Java 堆和永久代的详细信息，如当前使用率、当前使用的是哪种收集器等。
>
> 如果不使用这个命令，还可以使用 `-XX:+HeapDumpOnOutOfMemoryError` 参数来让虚拟机出现 OOM 的时候，自动生成 dump 文件。

命令格式：

```
jmap [option] VMID
```

常用参数：

- `option` - 选项参数
  - `-dump` - 生成堆转储快照。`-dump:live` 只保存堆中的存活对象。
  - `-finalizerinfo` - 显示在 F-Queue 队列等待执行 `finalizer` 方法的对象
  - `-heap` - 显示 Java 堆详细信息。
  - `-histo` - 显示堆中对象的统计信息，包括类、实例数量、合计容量。`-histo:live` 只统计堆中的存活对象。
  - `-permstat` - to print permanent generation statistics
  - `-F` - 当-dump 没有响应时，强制生成 dump 快照

【示例】

**（1）生成 heapdump 快照**

dump 堆到文件，format 指定输出格式，live 指明是活着的对象，file 指定文件名

```shell
$ jmap -dump:live,format=b,file=dump.hprof 28920
Dumping heap to /home/xxx/dump.hprof ...
Heap dump file created
```

dump.hprof 这个后缀是为了后续可以直接用 MAT(Memory Anlysis Tool)等工具打开。

**（2）查看实例数最多的类**

```shell
$ jmap -histo 29527 | head -n 6

 num     #instances         #bytes  class name
----------------------------------------------
   1:      13673280     1438961864  [C
   2:       1207166      411277184  [I
   3:       7382322      347307096  [Ljava.lang.Object;
```

**（3） 查看指定进程的堆信息**

注意：使用 CMS GC 情况下，`jmap -heap PID` 的执行有可能会导致 java 进程挂起。

```shell
$ jmap -heap 12379
Attaching to process ID 12379, please wait...
Debugger attached successfully.
Server compiler detected.
JVM version is 17.0-b16

using thread-local object allocation.
Parallel GC with 6 thread(s)

Heap Configuration:
   MinHeapFreeRatio = 40
   MaxHeapFreeRatio = 70
   MaxHeapSize      = 83886080 (80.0MB)
   NewSize          = 1310720 (1.25MB)
   MaxNewSize       = 17592186044415 MB
   OldSize          = 5439488 (5.1875MB)
   NewRatio         = 2
   SurvivorRatio    = 8
   PermSize         = 20971520 (20.0MB)
   MaxPermSize      = 88080384 (84.0MB)

Heap Usage:
PS Young Generation
Eden Space:
   capacity = 9306112 (8.875MB)
   used     = 5375360 (5.1263427734375MB)
   free     = 3930752 (3.7486572265625MB)
   57.761608714788736% used
From Space:
   capacity = 9306112 (8.875MB)
   used     = 3425240 (3.2665634155273438MB)
   free     = 5880872 (5.608436584472656MB)
   36.80634834397007% used
To Space:
   capacity = 9306112 (8.875MB)
   used     = 0 (0.0MB)
   free     = 9306112 (8.875MB)
   0.0% used
PS Old Generation
   capacity = 55967744 (53.375MB)
   used     = 48354640 (46.11457824707031MB)
   free     = 7613104 (7.2604217529296875MB)
   86.39733629427693% used
PS Perm Generation
   capacity = 62062592 (59.1875MB)
   used     = 60243112 (57.452308654785156MB)
   free     = 1819480 (1.7351913452148438MB)
   97.06831451706046% used
```

### jstack

> **[jstack(Stack Trace for java)](https://docs.oracle.com/en/java/javase/11/tools/jstack.html) 是 Java 堆栈跟踪工具**。jstack 用来打印目标 Java 进程中各个线程的栈轨迹，以及这些线程所持有的锁，并可以生成 java 虚拟机当前时刻的线程快照（一般称为 threaddump 或 javacore 文件）。
>
> **线程快照是当前虚拟机内每一条线程正在执行的方法堆栈的集合，生成线程快照的主要目的是定位线程出现长时间停顿的原因，如线程间死锁、死循环、请求外部资源导致的长时间等待等**。

线程出现停顿的时候通过 jstack 来查看各个线程的调用堆栈，就可以知道没有响应的线程到底在后台做什么事情，或者等待什么资源。 如果 java 程序崩溃生成 core 文件，jstack 工具可以用来获得 core 文件的 java stack 和 native stack 的信息，从而可以轻松地知道 java 程序是如何崩溃和在程序何处发生问题。另外，jstack 工具还可以附属到正在运行的 java 程序中，看到当时运行的 java 程序的 java stack 和 native stack 的信息, 如果现在运行的 java 程序呈现 hung 的状态，jstack 是非常有用的。

命令格式：

```shell
jstack [option] vmid
```

常用参数：

- `option` - 选项参数
  - `-F` - 当正常输出请求不被响应时，强制输出线程堆栈
  - `-l` - 除堆栈外，显示关于锁的附加信息
  - `-m` - 如果调用到本地方法的话，可以显示 C/C++的堆栈

【示例】

（1）找出某 Java 进程中最耗费 CPU 的 Java 线程

a) 找出 Java 进程

假设应用名称为 myapp：

```shell
$ jps | grep myapp
29527 myapp.jar
```

得到进程 ID 为 21711

b) 找出该进程内最耗费 CPU 的线程，可以使用 `ps -Lfp pid` 或者 `ps -mp pid -o THREAD, tid, time` 或者 `top -Hp pid`

![img](http://static.oschina.net/uploads/space/2014/0128/170402_A57i_111708.png)
TIME 列就是各个 Java 线程耗费的 CPU 时间，CPU 时间最长的是线程 ID 为 21742 的线程，用

```shell
printf "%x\n" 21742
```

得到 21742 的十六进制值为 54ee，下面会用到。

c) 使用 jstack 打印线程堆栈信息

下一步终于轮到 jstack 上场了，它用来输出进程 21711 的堆栈信息，然后根据线程 ID 的十六进制值 grep，如下：

```shell
$ jstack 21711 | grep 54ee
"PollIntervalRetrySchedulerThread" prio=10 tid=0x00007f950043e000 nid=0x54ee in Object.wait() [0x00007f94c6eda000]
```

可以看到 CPU 消耗在 PollIntervalRetrySchedulerThread 这个类的 Object.wait()，我找了下我的代码，定位到下面的代码：

```java
// Idle wait
getLog().info("Thread [" + getName() + "] is idle waiting...");
schedulerThreadState = PollTaskSchedulerThreadState.IdleWaiting;
long now = System.currentTimeMillis();
long waitTime = now + getIdleWaitTime();
long timeUntilContinue = waitTime - now;
synchronized(sigLock) {
	try {
    	if(!halted.get()) {
    		sigLock.wait(timeUntilContinue);
    	}
    }
	catch (InterruptedException ignore) {
    }
}
```

它是轮询任务的空闲等待代码，上面的 sigLock.wait(timeUntilContinue) 就对应了前面的 Object.wait()。

### jhat

> **jhat(JVM Heap Analysis Tool)，是虚拟机堆转储快照分析工具**。jhat 与 jmap 搭配使用，用来分析 jmap 生成的 dump 文件。jhat 内置了一个微型的 HTTP/HTML 服务器，生成 dump 的分析结果后，可以在浏览器中查看。
>
> 注意：一般不会直接在服务器上进行分析，因为 jhat 是一个耗时并且耗费硬件资源的过程，一般把服务器生成的 dump 文件，用 jvisualvm 、Eclipse Memory Analyzer、IBM HeapAnalyzer 等工具来分析。

命令格式：

```shell
jhat [dumpfile]
```

### jinfo

> **[jinfo(JVM Configuration info)](https://docs.oracle.com/en/java/javase/11/tools/jinfo.html)，是 Java 配置信息工具**。jinfo 用于实时查看和调整虚拟机运行参数。如传递给 Java 虚拟机的`-X`（即输出中的 jvm_args）、`-XX`参数（即输出中的 VM Flags），以及可在 Java 层面通过`System.getProperty`获取的`-D`参数（即输出中的 System Properties）。

之前的 `jps -v` 口令只能查看到显示指定的参数，如果想要查看未被显示指定的参数的值就要使用 jinfo 口令。

命令格式：

```shell
jinfo [option] pid
```

常用参数：

- `option` - 选项参数
  - `-flag` - 输出指定 args 参数的值
  - `-sysprops` - 输出系统属性，等同于 `System.getProperties()`

【示例】

```shell
$ jinfo -sysprops 29527
Attaching to process ID 29527, please wait...
Debugger attached successfully.
Server compiler detected.
JVM version is 25.222-b10
...
```

## 二、JVM GUI 工具

### jconsole

> **jconsole(Java Monitoring and Management Console) 是一种基于 JMX 的可视化监视与管理工具**。它的管理功能是针对 JMX MBean 进行管理，由于 MBean 可以使用代码、中间件服务器的管理控制台或所有符合 JMX 规范的软件进行访问。

> 注意：使用 jconsole 的前提是 Java 应用开启 JMX。

#### 开启 JMX

Java 应用开启 JMX 后，可以使用 `jconsole` 或 `jvisualvm` 进行监控 Java 程序的基本信息和运行情况。

开启方法是，在 java 指令后，添加以下参数：

```java
-Dcom.sun.management.jmxremote=true
-Dcom.sun.management.jmxremote.ssl=false
-Dcom.sun.management.jmxremote.authenticate=false
-Djava.rmi.server.hostname=127.0.0.1
-Dcom.sun.management.jmxremote.port=18888
```

- `-Djava.rmi.server.hostname` - 指定 Java 程序运行的服务器
- `-Dcom.sun.management.jmxremote.port` - 指定 JMX 服务监听端口

#### 连接 jconsole

![Connecting to a JMX Agent Using the JMX Service URL](https://docs.oracle.com/javase/8/docs/technotes/guides/management/figures/connectadv.gif)

进入 jconsole 应用后，可以看到以下 tab 页面。

- `概述` - 显示有关 Java VM 和监视值的概述信息。
- `内存` - 显示有关内存使用的信息。内存页相当于可视化的 `jstat` 命令。
- `线程` - 显示有关线程使用的信息。
- `类` - 显示有关类加载的信息。
- `VM 摘要` - 显示有关 Java VM 的信息。
- `MBean` - 显示有关 MBean 的信息。

### jvisualvm

> **jvisualvm(All-In-One Java Troubleshooting Tool) 是多合一故障处理工具**。它支持运行监视、故障处理、性能分析等功能。

> 注意：使用 jconsole 的前提是 Java 应用开启 JMX。

### MAT

[MAT](https://www.eclipse.org/mat/) 即 Eclipse Memory Analyzer Tool 的缩写。

MAT 本身也能够获取堆的二进制快照。该功能将借助 `jps` 列出当前正在运行的 Java 进程，以供选择并获取快照。由于 `jps` 会将自己列入其中，因此你会在列表中发现一个已经结束运行的 `jps` 进程。

MAT 可以独立安装（[官方下载地址](http://www.eclipse.org/mat/downloads.php)），也可以作为 Eclipse IDE 的插件安装。

#### MAT 配置

MAT 解压后，安装目录下有个 `MemoryAnalyzer.ini` 文件。

`MemoryAnalyzer.ini` 中有个重要的参数 `Xmx` 表示最大内存，默认为：`-vmargs -Xmx1024m`

如果试图用 MAT 导入的 dump 文件超过 1024 M，会报错：

```shell
An internal error occurred during: "Parsing heap dump from XXX"
```

此时，可以适当调整 `Xmx` 大小。如果设置的 `Xmx` 数值过大，本机内存不足以支撑，启动 MAT 会报错：

```
Failed to create the Java Virtual Machine
```

#### MAT 分析

![img](http://dunwu.test.upcdn.net/snap/20200308092746.png)

点击 Leak Suspects 可以进入内存泄漏页面。

（1）首先，可以查看饼图了解内存的整体消耗情况

![img](http://dunwu.test.upcdn.net/snap/20200308150556.png)

（2）缩小范围，寻找问题疑似点

![img](https://img-blog.csdn.net/20160223202154818)

可以点击进入详情页面，在详情页面 Shortest Paths To the Accumulation Point 表示 GC root 到内存消耗聚集点的最短路径，如果某个内存消耗聚集点有路径到达 GC root，则该内存消耗聚集点不会被当做垃圾被回收。

为了找到内存泄露，我获取了两个堆转储文件，两个文件获取时间间隔是一天（因为内存只是小幅度增长，短时间很难发现问题）。对比两个文件的对象，通过对比后的结果可以很方便定位内存泄露。

MAT 同时打开两个堆转储文件，分别打开 Histogram，如下图。在下图中方框 1 按钮用于对比两个 Histogram，对比后在方框 2 处选择 Group By package，然后对比各对象的变化。不难发现 heap3.hprof 比 heap6.hprof 少了 64 个 eventInfo 对象，如果对代码比较熟悉的话想必这样一个结果是能够给程序员一定的启示的。而我也是根据这个启示差找到了最终内存泄露的位置。
![img](https://img-blog.csdn.net/20160223203226362)

## 参考资料

- [《深入理解 Java 虚拟机》](https://item.jd.com/11252778.html)
- [JVM 性能调优监控工具 jps、jstack、jmap、jhat、jstat、hprof 使用详解](https://my.oschina.net/feichexia/blog/196575)
- [jconsole 官方文档](https://docs.oracle.com/javase/8/docs/technotes/guides/management/jconsole.html)
- [jconsole 工具使用](https://www.cnblogs.com/kongzhongqijing/articles/3621441.html)
- [jstat 命令查看 jvm 的 GC 情况](https://www.cnblogs.com/yjd_hycf_space/p/7755633.html)
- [利用内存分析工具（Memory Analyzer Tool，MAT）分析 java 项目内存泄露](https://blog.csdn.net/wanghuiqi2008/article/details/50724676)
