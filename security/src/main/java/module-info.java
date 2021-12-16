module security {
    requires image;
    requires gson;
    requires java.sql;
    requires java.desktop;
    requires java.prefs;
    requires com.miglayout.swing;
    requires com.google.common;
    opens com.udacity.catpoint.security.data to gson;
}