package org.jmol.popup;

import java.util.Properties;

import org.jmol.api.PlatformViewer;
import org.jmol.i18n.GT;
import org.jmol.viewer.Viewer;

import org.jmol.awtjs.swing.SC;

/**
 * An abstract popup class that is 
 * instantiated for a given platform and
 * context as one of:
 * 
 * <pre>
 * -- abstract JmolGenericPopup
 *   -- abstract JmolPopup
 *      -- AwtJmolPopup
 *      -- JSJmolPopup
 *   -- abstract ModelKitPopup
 *      -- AwtModelKitPopup
 *      -- JSModelKitPopup
 * </pre>
 * 
 */
public abstract class JmolGenericPopup extends GenericPopup {


  protected SC frankPopup;
  protected int nFrankList = 0;
  protected Viewer vwr;
  protected Properties menuText = new Properties();

  @Override
  public void jpiInitialize(PlatformViewer vwr, String menu) {
    boolean doTranslate = GT.setDoTranslate(true);
    PopupResource bundle = getBundle(menu);
    initialize((Viewer) vwr, bundle, bundle.getMenuName());
    GT.setDoTranslate(doTranslate);
  }
  
  abstract protected PopupResource getBundle(String menu);

  protected void initialize(Viewer vwr, PopupResource bundle, String title) {
    this.vwr = vwr;
    initSwing(title, bundle, vwr.html5Applet, vwr.isJSNoAWT, 
        vwr.getBooleanProperty("_signedApplet"), vwr.isWebGL);
  }

  @Override
  public void jpiShow(int x, int y) {
    if (!vwr.haveDisplay)
      return;
    thisx = x;
    thisy = y;
    show(x, y, false);
    if (x < 0 && showFrankMenu())
      return;
    appRestorePopupMenu();
    menuShowPopup(popupMenu, thisx, thisy);
  }

  protected boolean showFrankMenu() {
    // subclassed in JmolGenericPopup
    return true;
  }

  @Override
  public void jpiDispose() {
    helper.menuClearListeners(popupMenu);
    popupMenu = thisPopup = null;
  }

  @Override
  public SC jpiGetMenuAsObject() {
    return popupMenu;
  }

  @Override
  protected String appFixLabel(String label) {
    return label;
  }
  
  @Override
  protected boolean appGetBooleanProperty(String name) {
    return vwr.getBooleanProperty(name);
  }


  @Override
  protected boolean appRunSpecialCheckBox(SC item, String basename, String what,
                                         boolean TF) {
    if (appGetBooleanProperty(basename) == TF)
      return true;
    if (!basename.endsWith("P!"))
      return false;
    if (basename.indexOf("??") >= 0) {
      what = getUnknownCheckBoxScriptToRun(item, basename, what, TF);
    } else {
      if (!TF)
        return true;
      what = "set picking " + basename.substring(0, basename.length() - 2);
    }
    if (what != null)
      appRunScript(what);
    return true;
  }

  @Override
  protected void appRestorePopupMenu() {
    thisPopup = popupMenu;
  }

  @Override
  protected void appRunScript(String script) {
    vwr.evalStringQuiet(script);
  }


}
