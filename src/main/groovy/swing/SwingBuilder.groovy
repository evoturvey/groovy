/*
 * Copyright 2003-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package groovy.swing

import groovy.swing.factory.*
import java.awt.*
import java.lang.reflect.InvocationTargetException
import java.util.logging.Logger
import javax.swing.*
import javax.swing.table.TableColumn

/**
 * A helper class for creating Swing widgets using GroovyMarkup
 *
 * @author <a href="mailto:james@coredevelopers.net">James Strachan</a>
 * @version $Revision$
 */
public class SwingBuilder extends FactoryBuilderSupport {

    // Properties
    def constraints
    LinkedList containingWindows = new LinkedList()
    Map widgets = [:]

    // local fields
    private static final Logger LOG = Logger.getLogger(SwingBuilder.class.getName());
    // tracks all containing windows, for auto-owned dialogs
    private boolean headless = false
    private disposalClosures = []

    public SwingBuilder() {
        registerWidgets();
        headless = GraphicsEnvironment.isHeadless();
    }

    public Object getProperty(String name) {
        Object widget = widgets.get(name);
        if (widget == null) {
            return super.getProperty(name);
        }
        return widget;
    }

    protected void registerWidgets() {
        //
        // non-widget support classes
        //
        registerFactory("action", new ActionFactory());
        registerFactory("actions", new CollectionFactory());
        registerFactory("map", new MapFactory());
        registerBeanFactory("buttonGroup", ButtonGroup);
        addAttributeDelegate {builder, node, attributes ->
            if (attributes.containsKey("buttonGroup")) {
                def o = attributes.get("buttonGroup")
                if ((o instanceof ButtonGroup) && (node instanceof AbstractButton)) {
                    node.model.group = o
                    attributes.remove("buttonGroup")
                }
            }
        }

        //object id delegage, for propertyNotFound
        addAttributeDelegate {builder, node, attributes ->
            def theID = attributes.remove('id')
            if (theID) {
                widgets[theID] = node
            }
        }

        // binding related classes
        registerFactory("bind", new BindFactory());
        addAttributeDelegate(BindFactory.attributeDelegate)
        registerFactory("model", new ModelFactory());

        // ulimate pass through types
        registerFactory("widget", new WidgetFactory(Component, true)); 
        registerFactory("container", new WidgetFactory(Component, false));
        registerFactory("bean", new WidgetFactory(Object, true));


        //
        // standalone window classes
        //
        registerFactory("dialog", new DialogFactory());
        registerFactory("fileChooser", new ComponentFactory(JFileChooser));
        registerFactory("frame", new FrameFactory());
        registerFactory("optionPane", new ComponentFactory(JOptionPane));
        registerFactory("window", new WindowFactory());


        //
        // widgets
        //
        registerFactory("button", new RichActionWidgetFactory(JButton));
        registerFactory("checkBox", new RichActionWidgetFactory(JCheckBox));
        registerFactory("checkBoxMenuItem", new RichActionWidgetFactory(JCheckBoxMenuItem));
        registerFactory("menuItem", new RichActionWidgetFactory(JMenuItem));
        registerFactory("radioButton", new RichActionWidgetFactory(JRadioButton));
        registerFactory("radioButtonMenuItem", new RichActionWidgetFactory(JRadioButtonMenuItem));
        registerFactory("toggleButton", new RichActionWidgetFactory(JToggleButton));

        registerFactory("editorPane", new TextArgWidgetFactory(JEditorPane));
        registerFactory("label", new TextArgWidgetFactory(JLabel));
        registerFactory("passwordField", new TextArgWidgetFactory(JPasswordField));
        registerFactory("textArea", new TextArgWidgetFactory(JTextArea));
        registerFactory("textField", new TextArgWidgetFactory(JTextField));
        registerFactory("textPane", new TextArgWidgetFactory(JTextPane));

        registerBeanFactory("colorChooser", JColorChooser);
        registerFactory("comboBox", new ComboBoxFactory());
        registerFactory("desktopPane", new ComponentFactory(JDesktopPane));
        registerFactory("formattedTextField", new FormattedTextFactory());
        registerFactory("internalFrame", new InternalFrameFactory());
        registerFactory("layeredPane", new ComponentFactory(JLayeredPane));
        registerBeanFactory("list", JList);
        registerFactory("menu", new ComponentFactory(JMenu));
        registerFactory("menuBar", new ComponentFactory(JMenuBar));
        registerFactory("panel", new ComponentFactory(JPanel));
        registerFactory("popupMenu", new ComponentFactory(JPopupMenu));
        registerBeanFactory("progressBar", JProgressBar);
        registerBeanFactory("scrollBar", JScrollBar);
        registerFactory("scrollPane", new ScrollPaneFactory());
        registerFactory("separator", new SeparatorFactory());
        registerBeanFactory("slider", JSlider);
        registerBeanFactory("spinner", JSpinner);
        registerFactory("splitPane", new SplitPaneFactory());
        registerFactory("tabbedPane", new ComponentFactory(JTabbedPane));
        registerFactory("table", new TableFactory());
        registerBeanFactory("tableColumn", TableColumn);
        registerFactory("toolBar", new ComponentFactory(JToolBar));
        //registerBeanFactory("tooltip", JToolTip); // doesn't work, use toolTipText property
        registerBeanFactory("tree", JTree);
        registerFactory("viewport", new ComponentFactory(JViewport)); // sub class?


        //
        // MVC models
        //
        registerBeanFactory("boundedRangeModel", DefaultBoundedRangeModel);

        // spinner models
        registerBeanFactory("spinnerDateModel", SpinnerDateModel);
        registerBeanFactory("spinnerListModel", SpinnerListModel);
        registerBeanFactory("spinnerNumberModel", SpinnerNumberModel);

        // table models
        registerFactory("tableModel", new TableModelFactory());
        registerFactory("propertyColumn", new PropertyColumnFactory());
        registerFactory("closureColumn", new ClosureColumnFactory());


        //
        // Layouts
        //
        registerFactory("borderLayout", new LayoutFactory(BorderLayout));
        registerFactory("cardLayout", new LayoutFactory(CardLayout));
        registerFactory("flowLayout", new LayoutFactory(FlowLayout));
        registerFactory("gridBagLayout", new LayoutFactory(GridBagLayout));
        registerFactory("gridLayout", new LayoutFactory(GridLayout));
        registerFactory("overlayLayout", new LayoutFactory(OverlayLayout));
        registerFactory("springLayout", new LayoutFactory(SpringLayout));
        registerBeanFactory("gridBagConstraints", GridBagConstraints);
        registerBeanFactory("gbc", GridBagConstraints); // shortcut name
        // constraints delegate
        addAttributeDelegate {builder, node, attributes ->
            constraints = attributes.remove('constraints')
        }


        // Box layout and friends
        registerFactory("boxLayout", new BoxLayoutFactory());
        registerFactory("box", new BoxFactory());
        registerFactory("hbox", new HBoxFactory());
        registerFactory("hglue", new HGlueFactory());
        registerFactory("hstrut", new HStrutFactory());
        registerFactory("vbox", new VBoxFactory());
        registerFactory("vglue", new VGlueFactory());
        registerFactory("vstrut", new VStrutFactory());
        registerFactory("glue", new GlueFactory());
        registerFactory("rigidArea", new RigidAreaFactory());

        // table layout
        registerFactory("tableLayout", new TableLayoutFactory());
        registerFactory("tr", new TRFactory());
        registerFactory("td", new TDFactory());

    }

    public SwingBuilder edt(Closure c) {
        c.setDelegate(this);
        if (headless || SwingUtilities.isEventDispatchThread()) {
            c.call(this);
        } else {
            try {
                SwingUtilities.invokeAndWait(c.curry([this]));
            } catch (InterruptedException e) {
                throw new GroovyRuntimeException("interrupted swing interaction", e);
            } catch (InvocationTargetException e) {
                throw new GroovyRuntimeException("exception in event dispatch thread", e.getTargetException());
            }
        }
        return this;
    }

    public SwingBuilder doLater(Closure c) {
        c.setDelegate(this)
        if (headless) {
            c.call()
        } else {
            SwingUtilities.invokeLater(c.curry([this]))
        }
        return this
    }

    public SwingBuilder doOutside(Closure c) {
        c.setDelegate(this);
        new Thread(c.curry([this])).start()
        return this
    }

    public static SwingBuilder build(Closure c) {
        SwingBuilder builder = new SwingBuilder();
        return builder.edt(c);
    }

    public KeyStroke shortcut(key, modifier = 0) {
        return KeyStroke.getKeyStroke(key, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | modifier);
    }

    public KeyStroke shortcut(String key, modifier = 0) {
        KeyStroke ks = KeyStroke.getKeyStroke(key);
        if (ks == null) {
            return null;
        } else {
            return KeyStroke.getKeyStroke(ks.getKeyCode(), ks.getModifiers() | modifier | Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        }
    }

    public void addDisposalClosure(closure) {
        disposalClosures += closure
    }

    public void dispose() {
        disposalClosures.reverseEach {it()}
    }
}
