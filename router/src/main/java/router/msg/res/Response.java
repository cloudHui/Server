package router.msg.res;

public class Response<T> {
	private int ret;
	private String msg;
	private T data;

	public Response() {
	}

	public int getRet() {
		return this.ret;
	}

	public void setRet(int ret) {
		this.ret = ret;
	}

	public String getMsg() {
		return this.msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public T getData() {
		return this.data;
	}

	public void setData(T data) {
		this.data = data;
	}
}
