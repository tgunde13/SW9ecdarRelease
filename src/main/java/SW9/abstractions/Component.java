package SW9.abstractions;

import SW9.Ecdar;
import SW9.controllers.EcdarController;
import SW9.presentations.DropDownMenu;
import SW9.presentations.Grid;
import SW9.utility.UndoRedoStack;
import SW9.utility.colors.Color;
import SW9.utility.colors.EnabledColor;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import javafx.beans.property.*;
import javafx.beans.value.ObservableStringValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.util.Pair;

import javax.swing.event.ChangeListener;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Component extends VerificationObject implements DropDownMenu.HasColor {
    private static final AtomicInteger hiddenID = new AtomicInteger(0); // Used to generate unique IDs

    private static final String LOCATIONS = "locations";
    private static final String EDGES = "edges";
    private static final String DESCRIPTION = "description";
    private static final String X = "x";
    private static final String Y = "y";
    private static final String WIDTH = "width";
    private static final String HEIGHT = "height";
    private static final String INCLUDE_IN_PERIODIC_CHECK = "include_in_periodic_check";
    private static final String COLOR = "color";

    // Verification properties
    private final ObservableList<Location> locations = FXCollections.observableArrayList();
    private final ObservableList<Edge> edges = FXCollections.observableArrayList();
    private final ObservableList<String> inputStrings = FXCollections.observableArrayList();
    private final ObservableList<String> outputStrings = FXCollections.observableArrayList();
    private final StringProperty description = new SimpleStringProperty("");

    // Background check
    private final BooleanProperty includeInPeriodicCheck = new SimpleBooleanProperty(true);

    // Styling properties
    private final DoubleProperty x = new SimpleDoubleProperty(0d);
    private final DoubleProperty y = new SimpleDoubleProperty(0d);
    private final DoubleProperty width = new SimpleDoubleProperty(450d);
    private final DoubleProperty height = new SimpleDoubleProperty(600d);
    private final BooleanProperty declarationOpen = new SimpleBooleanProperty(false);

    private final BooleanProperty firsTimeShown = new SimpleBooleanProperty(false);

    public Component() {
        this(false);
    }

    public Component(final boolean doRandomColor) {
        this("Component" + hiddenID.getAndIncrement(), doRandomColor);
    }

    public Component(final String name, final boolean doRandomColor) {
        setName(name);

        if(doRandomColor) {
            // Color the new component in such a way that we avoid clashing with other components if possible
            final List<EnabledColor> availableColors = new ArrayList<>();
            EnabledColor.enabledColors.forEach(availableColors::add);
            Ecdar.getProject().getComponents().forEach(component -> {
                availableColors.removeIf(enabledColor -> enabledColor.color.equals(component.getColor()));
            });
            if (availableColors.size() == 0) {
                EnabledColor.enabledColors.forEach(availableColors::add);
            }
            final int randomIndex = (new Random()).nextInt(availableColors.size());
            final EnabledColor selectedColor = availableColors.get(randomIndex);
            setColorIntensity(selectedColor.intensity);
            setColor(selectedColor.color);
        }

        // Make initial location
        final Location initialLocation = new Location();
        initialLocation.setType(Location.Type.INITIAL);
        initialLocation.setColorIntensity(getColorIntensity());
        initialLocation.setColor(getColor());

        // Place in center
        initialLocation.setX(Grid.snap(getX() + getWidth() / 2));
        initialLocation.setY(Grid.snap(getY() + getHeight() / 2));

        locations.add(initialLocation);

        inputStrings.addListener((ListChangeListener<String>) c -> {
           System.out.println(inputStrings.toString());
        });
        outputStrings.addListener((ListChangeListener<String>) c -> {
            System.out.println(outputStrings.toString());
        });

        final javafx.beans.value.ChangeListener<Object> listener = (observable, oldValue, newValue) -> {
            updateIOList();
        };

        edges.addListener((ListChangeListener<Edge>) c -> {

            while(c.next()) {
                for (Edge e : c.getAddedSubList()) {
                    e.syncProperty().addListener(listener);
                    e.ioStatus.addListener(listener);
                }

                for (Edge e : c.getRemoved()) {
                    e.syncProperty().removeListener(listener);
                    e.ioStatus.removeListener(listener);
                }
            }
        });

        bindReachabilityAnalysis();
    }

    private void updateIOList() {
        List<String> localInputStrings = new ArrayList<>();
        List<String> localOutputStrings = new ArrayList<>();

        for (Edge edge : edges) {
            
            if(edge.getStatus() == EdgeStatus.INPUT){
                if(!localInputStrings.contains(edge.getSync())){
                    localInputStrings.add(edge.getSync());
                }
            } else if (edge.getStatus() == EdgeStatus.OUTPUT) {
                if(!localOutputStrings.contains(edge.getSync())){
                    localOutputStrings.add(edge.getSync());
                }
            }
        }

        inputStrings.setAll(localInputStrings);

        outputStrings.setAll(localOutputStrings);
    }

    public Component(final JsonObject object) {
        hiddenID.incrementAndGet();
        setFirsTimeShown(true);
        deserialize(object);
        bindReachabilityAnalysis();
    }

    /**
     * Get all locations in this, but the initial location (if one exists).
     * O(n), n is # of locations in component.
     * @return all but the initial location
     */
    public List<Location> getAllButInitialLocations() {
        final List<Location> locations = new ArrayList<>();
        locations.addAll(getLocations());

        // Remove initial location
        final Location initLoc = getInitialLocation();
        if (initLoc != null) {
            locations.remove(getInitialLocation());
        }

        return locations;
    }

    public ObservableList<Location> getLocations() {
        return locations;
    }

    public boolean addLocation(final Location location) {
        return locations.add(location);
    }

    public boolean removeLocation(final Location location) {
        return locations.remove(location);
    }

    public ObservableList<Edge> getEdges() {
        return edges;
    }

    public boolean addEdge(final Edge edge) {
        return edges.add(edge);
    }

    public boolean removeEdge(final Edge edge) {
        return edges.remove(edge);
    }

    public List<Edge> getRelatedEdges(final Location location) {
        final ArrayList<Edge> relatedEdges = new ArrayList<>();

        edges.forEach(edge -> {
            if(location.equals(edge.getSourceLocation()) ||location.equals(edge.getTargetLocation())) {
                relatedEdges.add(edge);
            }
        });

        return relatedEdges;
    }

    public double getX() {
        return x.get();
    }

    public void setX(final double x) {
        this.x.set(x);
    }

    public DoubleProperty xProperty() {
        return x;
    }

    public double getY() {
        return y.get();
    }

    public void setY(final double y) {
        this.y.set(y);
    }

    public DoubleProperty yProperty() {
        return y;
    }

    public double getWidth() {
        return width.get();
    }

    public void setWidth(final double width) {
        this.width.set(width);
    }

    public DoubleProperty widthProperty() {
        return width;
    }

    public double getHeight() {
        return height.get();
    }

    public void setHeight(final double height) {
        this.height.set(height);
    }

    public DoubleProperty heightProperty() {
        return height;
    }

    public boolean isDeclarationOpen() {
        return declarationOpen.get();
    }

    public BooleanProperty declarationOpenProperty() {
        return declarationOpen;
    }

    /**
     * Gets the initial location.
     * Done with linear search.
     * O(n), where n is number of locations in this.
     * @return the initial location, or null if there is none
     */
    public Location getInitialLocation() {
        for (final Location loc : getLocations()) {
            if (loc.getType() == Location.Type.INITIAL) {
                return  loc;
            }
        }

        return null;
    }

    /**
     * Sets current initial location (if one exists) to no longer initial.
     * Then sets a new initial location.
     * O(n), where n is number of locations in this.
     * @param initialLocation new initial location.
     */
    public void setInitialLocation(final Location initialLocation) {
        // Make current initial location no longer initial
        final Location currentInitLoc = getInitialLocation();
        if (currentInitLoc != null) {
            currentInitLoc.setType(Location.Type.NORMAL);
        }

        initialLocation.setType(Location.Type.INITIAL);
    }

    public Edge getUnfinishedEdge() {
        for (final Edge edge : edges) {
            if (edge.getTargetLocation() == null)
                return edge;
        }
        return null;
    }

    public boolean isFirsTimeShown() {
        return firsTimeShown.get();
    }

    public BooleanProperty firsTimeShownProperty() {
        return firsTimeShown;
    }

    public void setFirsTimeShown(final boolean firsTimeShown) {
        this.firsTimeShown.set(firsTimeShown);
    }

    public boolean isIncludeInPeriodicCheck() {
        return includeInPeriodicCheck.get();
    }

    public BooleanProperty includeInPeriodicCheckProperty() {
        return includeInPeriodicCheck;
    }

    public void setIncludeInPeriodicCheck(final boolean includeInPeriodicCheck) {
        this.includeInPeriodicCheck.set(includeInPeriodicCheck);
    }

    @Override
    public JsonObject serialize() {
        final JsonObject result = super.serialize();

        final JsonArray locations = new JsonArray();
        getLocations().forEach(location -> locations.add(location.serialize()));
        result.add(LOCATIONS, locations);

        final JsonArray edges = new JsonArray();
        getEdges().forEach(edge -> edges.add(edge.serialize()));
        result.add(EDGES, edges);

        result.addProperty(DESCRIPTION, getDescription());

        result.addProperty(X, getX());
        result.addProperty(Y, getY());
        result.addProperty(WIDTH, getWidth());
        result.addProperty(HEIGHT, getHeight());

        result.addProperty(COLOR, EnabledColor.getIdentifier(getColor()));

        result.addProperty(INCLUDE_IN_PERIODIC_CHECK, isIncludeInPeriodicCheck());

        return result;
    }

    @Override
    public void deserialize(final JsonObject json) {
        super.deserialize(json);

        json.getAsJsonArray(LOCATIONS).forEach(jsonElement -> {
            final Location newLocation = new Location((JsonObject) jsonElement);
            locations.add(newLocation);
        });

        json.getAsJsonArray(EDGES).forEach(jsonElement -> {
            final Edge newEdge = new Edge((JsonObject) jsonElement, this);
            edges.add(newEdge);
        });

        setDescription(json.getAsJsonPrimitive(DESCRIPTION).getAsString());

        setX(json.getAsJsonPrimitive(X).getAsDouble());
        setY(json.getAsJsonPrimitive(Y).getAsDouble());
        setWidth(json.getAsJsonPrimitive(WIDTH).getAsDouble());
        setHeight(json.getAsJsonPrimitive(HEIGHT).getAsDouble());

        final EnabledColor enabledColor = EnabledColor.fromIdentifier(json.getAsJsonPrimitive(COLOR).getAsString());
        if (enabledColor != null) {
            setColorIntensity(enabledColor.intensity);
            setColor(enabledColor.color);
        }

        setIncludeInPeriodicCheck(json.getAsJsonPrimitive(INCLUDE_IN_PERIODIC_CHECK).getAsBoolean());
    }

    /**
     * Dyes the component and its locations.
     * @param color the color to dye with
     * @param intensity the intensity of the color
     */
    public void dye(final Color color, final Color.Intensity intensity) {
        final Color previousColor = colorProperty().get();
        final Color.Intensity previousColorIntensity = colorIntensityProperty().get();

        final Map<Location, Pair<Color, Color.Intensity>> previousLocationColors = new HashMap<>();

        for (final Location location : getLocations()) {
            if (!location.getColor().equals(previousColor)) continue;
            previousLocationColors.put(location, new Pair<>(location.getColor(), location.getColorIntensity()));
        }

        UndoRedoStack.pushAndPerform(() -> { // Perform
            // Color the component
            setColorIntensity(intensity);
            setColor(color);

            // Color all of the locations
            previousLocationColors.keySet().forEach(location -> {
                location.setColorIntensity(intensity);
                location.setColor(color);
            });
        }, () -> { // Undo
            // Color the component
            setColorIntensity(previousColorIntensity);
            setColor(previousColor);

            // Color the locations accordingly to the previous color for them
            previousLocationColors.keySet().forEach(location -> {
                location.setColorIntensity(previousLocationColors.get(location).getValue());
                location.setColor(previousLocationColors.get(location).getKey());
            });
        }, String.format("Changed the color of %s to %s", this, color.name()), "color-lens");
    }

    private void bindReachabilityAnalysis() {
        locations.addListener((ListChangeListener<? super Location>) c -> EcdarController.runReachabilityAnalysis());
        edges.addListener((ListChangeListener<? super Edge>) c -> EcdarController.runReachabilityAnalysis());
        declarationsTextProperty().addListener((observable, oldValue, newValue) -> EcdarController.runReachabilityAnalysis());
        includeInPeriodicCheckProperty().addListener((observable, oldValue, newValue) -> EcdarController.runReachabilityAnalysis());
    }

    public String getDescription() {
        return description.get();
    }

    public void setDescription(final String description) {
        this.description.set(description);
    }

    public StringProperty descriptionProperty() {
        return description;
    }

    public ObservableList<String> getInputStrings() {
        return inputStrings;
    }

    public ObservableList<String> getOutputStrings() {
        return outputStrings;
    }
}
