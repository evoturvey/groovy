/*
 * Copyright 2007 the original author or authors.
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
package groovy.swing.factory

import javax.swing.border.TitledBorder
import java.awt.Color
import javax.swing.border.Border
import java.awt.Font


/**
 * The node must be called with either a value arugment or a title: attribute. <br />
 * The following attributes are optional. <br />
 *    position: one of "default", "aboveTop", "top", "belowTop", "aboveBottom", "bottom", "belowBottom", or a constant from javax.swing.border.TitledBorder
 *    justification: one of "default", "left", "center", "right", "leading", "trailing", or a constant from javax.swing.border.TitledBorder
 *    border: javax.swing.Border, some other border, if unset the look and feel default will be used (re
 *    color: java.awt.Color the color of the text for the title
 *    font: java.awt.Font the font of the text for the title
 */
class TitledBorderFactory extends SwingBorderFactory {

    static final Map positions = [
        'default':    TitledBorder.DEFAULT_POSITION,
        aboveTop:    TitledBorder.ABOVE_TOP,
        top:          TitledBorder.TOP,
        belowTop:    TitledBorder.BELOW_TOP,
        aboveBottom: TitledBorder.ABOVE_BOTTOM,
        bottom:       TitledBorder.BOTTOM,
        belowBottom: TitledBorder.BELOW_BOTTOM,
    ]

    static final Map justifications = [
        'default': TitledBorder.DEFAULT_JUSTIFICATION,
        left:      TitledBorder.LEFT,
        center:    TitledBorder.CENTER,
        right:     TitledBorder.RIGHT,
        leading:   TitledBorder.LEADING,
        trailing:  TitledBorder.TRAILING,
    ]


    public Object newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attributes) {
        builder.context.applyBorderToParent = attributes.remove('parent')
        
        String title
        if (value) {
            title = value as String
            if (attributes.containsKey("title")) {
                throw new RuntimeException("$name cannot have both a value attribute and an attribute title:")
            }
        } else {
            title = attributes.remove("title") as String
        }
        TitledBorder border = new TitledBorder(title)

        def position = attributes.remove("position")
        position = positions[position] ?: position
        if (position instanceof Integer) { border.setTitlePosition(position) }

        def justificaiton = attributes.remove("justificaiton")
        justificaiton = positions[justificaiton] ?: justificaiton
        if (justificaiton instanceof Integer) { border.setTitleJustification(justificaiton) }

        Border otherBorder = attributes.remove("border")
        if (otherBorder != null) { border.setBorder(otherBorder) }

        Color color = attributes.remove("color")
        if (color) { border.setTitleColor(color) }

        Font font = attributes.remove("font")
        if (font) { border.setTitleFont(font) }

        return border
    }

}