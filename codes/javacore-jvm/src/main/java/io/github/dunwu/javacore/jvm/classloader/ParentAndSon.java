package io.github.dunwu.javacore.jvm.classloader;

/**
 * @author Zhang Peng
 * @since 2018/4/16
 */
public class ParentAndSon {

	public static void main(String[] args) {
		System.out.println(Sub.B);
	}

	static class Parent {

		public static int A = 1;

		static {
			A = 2;
		}
	}

	static class Sub extends Parent {

		public static int B = A;

	}

}
