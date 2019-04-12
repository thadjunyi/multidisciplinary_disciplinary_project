package Helper;
import javafx.scene.control.*;
import java.lang.*;

public class PrintManager {

  private static TextArea textArea = new TextArea();

  public void setText(String str) {
		this.textArea.setText(str);
    // this.textArea.appendText("");
	}

  public String getText() {
    return this.textArea.getText();
  }

  public TextArea getTextArea() {
      return this.textArea;
  }

}
