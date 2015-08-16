package zq.java.util;


import java.io.File;
import java.util.NoSuchElementException;


public class SystemArgs {


	public static boolean isNumber(String arg) {

		try {
			Double.parseDouble(arg);
			return true;
		} catch (Exception e) {
			return false;
		}
	}


	public static boolean isExistedFile(String arg) {

		File file = new File(arg);
		return file.exists() && file.isFile();
	}


	private static boolean isFunctionFormat(String arg) {

		return arg.startsWith("-") && !isNumber(arg);
	}


	private final String[] mArgs;
	private int mCurrentIndex;


	public SystemArgs(String[] args) {

		mArgs = args;
		mCurrentIndex = -1;
	}


	public int getLength() {

		return mArgs.length;
	}


	public int getCurrent() {

		return mCurrentIndex;
	}


	public boolean atEnd() {

		return mCurrentIndex >= mArgs.length;
	}


	public boolean hasMore() {

		return mCurrentIndex + 1 < mArgs.length;
	}


	public boolean hasMoreFunctions() {

		int index = mCurrentIndex;
		if (nextFunction() == null)
			return false;
		mCurrentIndex = index;
		return true;
	}


	public boolean hasMoreParam() {

		int index = mCurrentIndex;
		if (nextParam() == null)
			return false;
		mCurrentIndex = index;
		return true;
	}


	public String current() {

		return mCurrentIndex < mArgs.length ? mArgs[mCurrentIndex] : null;
	}


	public String next() {

		return mCurrentIndex + 1 < mArgs.length ? mArgs[++mCurrentIndex] : null;
	}


	public String nextFunction() {

		int index = mCurrentIndex;
		while (null != next()) {
			if (isFunctionFormat(mArgs[mCurrentIndex]))
				return mArgs[mCurrentIndex];
		}
		mCurrentIndex = index;
		return null;
	}


	public String nextFunctionOrThrow() throws NoSuchElementException {

		String funParam = nextFunction();
		if (funParam != null)
			return funParam;
		else
			throw new NoSuchElementException("No more functions");
	}


	public String nextParam() {

		int index = mCurrentIndex;
		if (null != next()) {
			if (!isFunctionFormat(mArgs[mCurrentIndex]))
				return mArgs[mCurrentIndex];
		}
		mCurrentIndex = index;
		return null;
	}


	public String nextParamOrThrow() throws NoSuchElementException {

		String param = nextParam();
		if (param != null)
			return param;
		else
			throw new NoSuchElementException("No more params");
	}


	@Override
	public synchronized String toString() {

		StringBuilder sb = new StringBuilder();

		// save current index
		final int index = mCurrentIndex;

		mCurrentIndex = -1;
		String funName, param;
		while ((param = nextParam()) != null) {
			sb.append(param);
			sb.append(" ");
		}
		sb.append("\n");
		while ((funName = nextFunction()) != null) {
			sb.append(funName);
			sb.append("   ");
			while ((param = nextParam()) != null) {
				sb.append(param);
				sb.append(" ");
			}
			sb.append("\n");
		}

		// restore current index
		mCurrentIndex = index;

		return sb.toString();
	}
}
