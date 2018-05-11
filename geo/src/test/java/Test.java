import java.io.IOException;
import ru.org.openam.geo.Client;

public class Test {

	@org.junit.Test
	public void test() throws IOException{
		Client.getList("85.21.96.129");
		Client.getList("apple.com");
		Client.getList("xxlbook.ru");
		Client.getList("svn.openam.org.ru");
		Client.getList("46.229.140.100");
		Client.getList("127.0.0.1");
		Client.getList("10.0.0.1");
		Client.getList("172.18.2.1");
		Client.getList(" ");
		Client.getList("");
		Client.getList("sdfsdf");
		Client.getList(null); 
		Client.getList("194.190.23.105");
		System.out.println(Client.get("194.190.23.105").l.distance(Client.get("185.50.24.83").l));
		System.out.println(Client.get("194.190.23.105").l.distance(Client.get("85.21.96.129").l));
		System.out.println(Client.get("194.190.23.105").l.distance(Client.get("46.229.140.100").l));
//		ByteArrayOutputStream baos = new ByteArrayOutputStream();
//	    ObjectOutputStream oos = new ObjectOutputStream(baos);
//	    oos.writeObject(Client.get("85.21.96.129"));
//	    oos.close();
	}
}
