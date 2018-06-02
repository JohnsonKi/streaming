package practice.spark.base;

import java.io.Serializable;
import java.util.Map;

public class AnnotatedMessageBean implements Serializable {

	private static final long serialVersionUID = - 2022345678L;

	private Map<String ,?> annotetion;
	private byte[] message;


	public Map<String, ?> getAnnotetion() {
		return annotetion;
	}
	public void setAnnotetion(Map<String, ?> annotetion) {
		this.annotetion = annotetion;
	}
	public byte[] getMessage() {
		return message;
	}
	public void setMessage(byte[] message) {
		this.message = message;
	}




}
