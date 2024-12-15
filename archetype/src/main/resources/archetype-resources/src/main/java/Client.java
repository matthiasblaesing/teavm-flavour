package ${package};

import org.teavm.flavour.templates.BindTemplate;
import org.teavm.flavour.widgets.ApplicationTemplate;
import org.teavm.jso.browser.Window;

@BindTemplate("templates/client.html")
public class Client extends ApplicationTemplate {
  private String userName = "";

  public static void main(String[] args) {
    Window.current().getDocument().getElementById("application-content").clear();
    Client client = new Client();
    client.bind("application-content");
  }

  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }
}
