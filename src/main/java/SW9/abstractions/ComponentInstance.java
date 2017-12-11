package SW9.abstractions;

import SW9.utility.colors.Color;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Instance of a component.
 */
public class ComponentInstance {
    private final ObjectProperty<Component> component = new SimpleObjectProperty<>();
    private final ObjectProperty<Color> color = new SimpleObjectProperty<>();
    private final ObjectProperty<Color.Intensity> colorIntensity = new SimpleObjectProperty<>();
    private final Box box = new Box();
    private final StringProperty id = new SimpleStringProperty("");

    public ComponentInstance() {

    }

    public Component getComponent() {
        return component.get();
    }

    public void setComponent(final Component component) {
        this.component.set(component);
    }

    public ObjectProperty<Color> getColor() {
        return color;
    }

    public ObjectProperty<Color.Intensity> getColorIntensity() {
        return colorIntensity;
    }

    public Box getBox() {
        return box;
    }

    public String getId() {
        return id.get();
    }

    public StringProperty getIdProperty() {
        return id;
    }
}
