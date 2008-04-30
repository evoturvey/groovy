/**
 * Created by IntelliJ IDEA.
 * User: Danno.Ferrin
 * Date: Apr 26, 2008
 * Time: 8:31:21 AM
 */
package swing.greet

import java.awt.Cursor
import java.beans.PropertyChangeListener
import javax.swing.DefaultComboBoxModel
import javax.swing.ListCellRenderer
import javax.swing.WindowConstants
import javax.swing.SwingConstants

lookAndFeel('nimbus')
//lookAndFeel('metal', boldFonts: false)

actions() {
    loginAction = action(
        name: 'Login',
        closure: controller.&login
    )

    filterTweets = action(
        name: 'Filter',
        closure: controller.&filterTweets
    )

    userSelected = action(
        name: 'Select User',
        closure: controller.&userSelected
    )
}

tweetLineFont = new java.awt.Font("Ariel", 0, 12)
tweetLine = panel(border: emptyBorder(3), opaque:false) {
    gridBagLayout()
    //tweetIcon = label(anchor: BASELINE, insets: [3, 3, 3, 3])
    tweetIcon = label(verticalTextPosition:SwingConstants.BOTTOM,
        horizontalTextPosition:SwingConstants.CENTER,
        anchor: CENTER, insets: [3, 3, 3, 3])
    tweetText = textArea(rows: 4, lineWrap: true, wrapStyleWord: true,
        opaque: false, editable: false, font: tweetLineFont,
        gridwidth: REMAINDER, weightx: 1.0, fill: BOTH, insets: [3, 3, 3, 3])
}
tweetRenderer = {list, tweet, index, isSelected, isFocused ->
    if (tweet) {
        tweetIcon.icon = controller.api.imageMap[tweet.user.profile_image_url as String]
        tweetIcon.text = tweet.user.screen_name
        tweetText.text = tweet.text
    } else {
        tweetIcon.icon = null
        tweetIcon.text = null
        tweetText.text = null
    }
    tweetLine
} as ListCellRenderer


userCell = label(border: emptyBorder(3))
userCellRenderer = {list, user, index, isSelected, isFocused ->
    if (user) {
        userCell.icon = controller.api.imageMap[user.profile_image_url as String]
        userCell.text = "<html>$user.screen_name<br>$user.name<br>$user.location<br>"
    } else {
        userCell.icon = null
        userCell.text = null
    }
    userCell
} as ListCellRenderer

greetFrame = frame(title: "Greet - A Groovy Twitter Client",
    defaultCloseOperation: javax.swing.JFrame.DISPOSE_ON_CLOSE, size: [320, 480]) {
    panel(cursor: bind(source: controller, sourceProperty: 'allowSelection',
        converter: {it ? null : Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)})
    ) {

        gridBagLayout()
        users = comboBox(renderer: userCellRenderer, action: userSelected,
            enabled: bind(source: controller, sourceProperty: 'allowSelection'),
            gridwidth: REMAINDER, insets: [6, 6, 3, 6], fill: HORIZONTAL)
        label('Search:', insets: [3, 6, 3, 3])
        searchField = textField(columns: 20, action: filterTweets,
            enabled: bind(source: controller, sourceProperty: 'allowSelection'),
            insets: [3, 3, 3, 3], weightx: 1.0, fill: HORIZONTAL)
        button(action: filterTweets,
            enabled: bind(source: controller, sourceProperty: 'allowSelection'),
            gridwidth: REMAINDER, insets: [3, 3, 3, 6]
        )
        tabbedPane(gridwidth: REMAINDER, weighty: 1.0, fill: BOTH) {
            scrollPane(title: 'Timeline') {
                timelinePane = panel() {

                    bg = imagePainter()
                    label("Look at me!")
                    list(visibleRowCount: 20, cellRenderer: tweetRenderer,
                    listData: bind(source: controller, sourceProperty: 'timeline'), opaque:false)
                    label("More!")
                }
            }
            scrollPane(title: 'Tweets') {
                tweetList = list(visibleRowCount: 20, cellRenderer: tweetRenderer,
                    listData: bind(source: controller, sourceProperty: 'tweets'))
            }
        }
        separator(fill: HORIZONTAL, gridwidth: REMAINDER)
        statusLine = label(text: bind(source: controller.api, sourceProperty: 'status'),
            gridwidth: REMAINDER, insets: [3, 6, 3, 6], anchor: WEST
        )
    }

    loginDialog = dialog(
        title: "Login to Greet", pack: true, resizable: false,
        defaultCloseOperation: WindowConstants.DISPOSE_ON_CLOSE)
        {
            panel(border: emptyBorder(3),
                cursor: bind(source: controller, sourceProperty: 'allowLogin',
                    converter: {it ? null : Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)})
            ) {
                gridBagLayout()
                label("@@",
                    anchor: EAST, insets: [3, 3, 3, 3])
                twitterNameField = textField(columns: 20,
                    enabled: bind(source: controller, sourceProperty: 'allowLogin'),
                    gridwidth: REMAINDER, insets: [3, 3, 3, 3])
                label("Password:",
                    anchor: EAST, insets: [3, 3, 3, 3])
                twitterPasswordField = passwordField(columns: 20,
                    enabled: bind(source: controller, sourceProperty: 'allowLogin'),
                    gridwidth: REMAINDER, insets: [3, 3, 3, 3])
                panel()
                button(loginAction, defaultButton: true,
                    enabled: bind(source: controller, sourceProperty: 'allowLogin'),
                    anchor: EAST, insets: [3, 3, 3, 3])
            }
        }

}

controller.addPropertyChangeListener("friends", {evt ->
    view.edt { users.model = new DefaultComboBoxModel(evt.newValue as Object[]) }
} as PropertyChangeListener)

controller.addPropertyChangeListener("timeline", {evt ->
    println bg.properties
//    timelinePane.backgroundPainter = new org.jdesktop.swingx.painter.ImagePainter(new URL(controller.tweets[0].user.profile_image_url as String))
    timelinePane.backgroundPainter = new org.jdesktop.swingx.painter.ImagePainter(new URL("http://s3.amazonaws.com/twitter_production/profile_background_images/2382980/oct_004.jpg"))
} as PropertyChangeListener)
//    view.edt {
//        for (tweet in controller.tweets) {
//            panel {
//            }
//        }
//    }
//
//} as PropertyChangeListener)

