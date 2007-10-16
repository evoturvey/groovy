/**
 * Created by IntelliJ IDEA.
 * User: Danno
 * Date: Oct 15, 2007
 * Time: 10:46:15 AM
 * To change this template use File | Settings | File Templates.
 */
package groovy.swing

import javax.swing.LookAndFeel
import javax.swing.UIManager
import javax.swing.plaf.metal.MetalLookAndFeel
import javax.swing.plaf.metal.MetalTheme
import javax.swing.plaf.metal.OceanTheme
import javax.swing.plaf.metal.DefaultMetalTheme


class LookAndFeelHelper {

    // protected so you can subclass and replace the singleton
    protected static LookAndFeelHelper instance;

    public static LookAndFeelHelper getInstance() {
        return instance ?: (instance = new LookAndFeelHelper())
    }

    private Map lafCodeNames = [
        // stuff built into various JDKs
        metal   : 'javax.swing.plaf.metal.MetalLookAndFeel',
        nimbus  : 'sun.swing.plaf.nimbus.NimbusLookAndFeel',
        mac     : 'apple.laf.AquaLookAndFeel',
        motif   : 'com.sun.java.swing.plaf.motif.MotifLookAndFeel',
        windows : 'com.sun.java.swing.plaf.windows.WindowsLookAndFeel',
        win2k   : 'com.sun.java.swing.plaf.windows.WindowsClassicLookAndFeel',
        gtk     : 'com.sun.java.swing.plaf.gtk.GTKLookAndFeel',
        synth   : 'javax.swing.plaf.synth.SynthLookAndFeel',

        // generic aliases in UIManager
        system        : UIManager.getSystemLookAndFeelClassName(),
        crossPlatform : UIManager.getCrossPlatformLookAndFeelClassName(),

        // jgoodies, requires external library
        plastic   : 'com.jgoodies.plaf.plastic.PlasticLookAndFeel',
        plastic3D : 'com.jgoodies.plaf.plastic.Plastic3DLookAndFeel',
        plasticXP : 'com.jgoodies.plaf.plastic.PlasticXPLookAndFeel',

        // substance, requires external library
        substance : 'org.jvnet.substance.SubstanceLookAndFeel',

        // napkin, requires external library
        napkin : 'net.sourceforge.napkinlaf.NapkinLookAndFeel'
    ]

    public String addLookAndFeelAlias(String alias, String className) {
        lafCodeNames[alias] = className
    }

    private Map extendedAttributes = [
        'javax.swing.plaf.metal.MetalLookAndFeel' : [
            theme : { laf, theme ->
                if (!(theme instanceof MetalTheme)) {
                    if (theme == 'ocean') {
                        theme = new OceanTheme()
                    } else if (theme == 'steel') {
                        theme = new DefaultMetalTheme();
                    } else {
                        theme = Class.forName(theme as String).newInstance()
                    }
                };
                MetalLookAndFeel.currentTheme = theme
            },
            boldFonts : { laf, bold -> UIManager.put('swing.boldMetal', bold as Boolean) }
        ],
        'org.jvnet.substance.SubstanceLookAndFeel' : [
            theme: { laf, theme -> laf.currentTheme = theme },
            skin: { laf, skin -> laf.skin = skin },
            watermark : { laf, watermark -> laf.currentWatermark = watermark },
        ],
        'javax.swing.plaf.synth.SynthLookAndFeel' : [
            styleFactory: { laf, styleFactyory -> laf.styleFactory = styleFactory }
            // any more complex init should be done in the init closure
        ]
    ]

    public String addLookAndFeelAttributeHandler(String className, String attr, Closure handler) {
        Map attrs = extendedAttributes[className]
        if (attrs == null) {
            attrs = [:]
            extendedAttributes[className] = attrs
        }
        attrs[attr] = handler
    }


    public boolean isLeaf() {
        return true
    }

    public LookAndFeel lookAndFeel(Object value, Map attributes, Closure initClosure) {
        LookAndFeel lafInstance
        String lafClassName

        if ((value instanceof Closure) && (initClosure == null)) {
            initClosure = value
            value = null
        }
        if (value == null) {
            value = attributes.remove('lookAndFeel')
        }
        if (FactoryBuilderSupport.checkValueIsTypeNotString(value, 'lookAndFeel', LookAndFeel)) {
            lafInstance = value
            lafClassName = lafInstance.class.name
        } else if (value != null) {
            lafClassName = lafCodeNames[value] ?: value
            lafInstance = (lafClassName as Class).newInstance()
        }

        // assume all configuration must be done prior to LAF being installed
        Map possibleAttributes = extendedAttributes[lafClassName] ?: [:]

        attributes.each {k, v ->
            if (possibleAttributes[k]) {
                possibleAttributes[k](lafInstance, v)
            } else {
                String attrs = possibleAttributes.keySet() as String
                throw new RuntimeException("SwingBuilder initialization for the Look and Feel Class $lafClassName only accepts the following attributes: $attrs")
            }
        }

        if (initClosure) {
            initClosure.call(lafInstance)
        }

        UIManager.setLookAndFeel(lafInstance)

        return lafInstance
    }
}