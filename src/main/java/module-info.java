module GIBaker.main {
    //here declares modules requires in the project
    requires javafx.controls;
    requires javafx.fxml;
    requires kotlin.stdlib; //kotlin support
    requires java.desktop;
    requires jogl.all; // jogl dependencies
    requires lombok;
    requires com.google.gson;
    requires commons.math3;
    requires com.google.common;
    requires java.compiler;

    /*opens org.openjfx to javafx.fxml;
    exports org.openjfx;*/
    // export package has app entry point, so that can launch com.myna.fx.FxApplication.main
    // check reference: https://stackoverflow.com/questions/67854139/javafx-warning-unsupported-javafx-configuration-classes-were-loaded-from-unna
    //exports com.myna.fx to javafx.graphics;
}