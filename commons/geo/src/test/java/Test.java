import java.io.IOException;
import ru.org.openam.geo.Client;

public class Test {

	@org.junit.Test
	public void test() throws IOException{
		System.out.println(Client.get("85.21.96.129").l.getLocation().getLatitude());
		System.out.println(Client.get("85.21.96.129"));
		System.out.println(Client.get("apple.com"));
		System.out.println(Client.get("xxlbook.ru"));
		System.out.println(Client.get("svn.openam.org.ru"));
		System.out.println(Client.get("46.229.140.100"));
		System.out.println(Client.get("127.0.0.1"));
		System.out.println(Client.get("10.0.0.1"));
		System.out.println(Client.get("172.18.2.1"));
		System.out.println(Client.get(" "));
		System.out.println(Client.get(""));
		System.out.println(Client.get("sdfsdf"));
		System.out.println(Client.getList(null)); 
		System.out.println(Client.get("194.190.23.105"));
//		System.out.println(Client.get("194.190.23.105").l.distance(Client.get("185.50.24.83").l));
//		System.out.println(Client.get("194.190.23.105").l.distance(Client.get("85.21.96.129").l));
//		System.out.println(Client.get("194.190.23.105").l.distance(Client.get("46.229.140.100").l));
//		ByteArrayOutputStream baos = new ByteArrayOutputStream();
//	    ObjectOutputStream oos = new ObjectOutputStream(baos);
//	    oos.writeObject(Client.get("85.21.96.129"));
//	    oos.close();
	}
}
