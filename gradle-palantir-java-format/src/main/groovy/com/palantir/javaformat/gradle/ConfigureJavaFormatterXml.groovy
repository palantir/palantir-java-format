package com.palantir.javaformat.gradle

class ConfigureJavaFormatterXml {
    static void configure(Node rootNode, Optional<List<URI>> uris) {
        def settings = matchOrCreateChild(rootNode, 'component', [name: 'PalantirJavaFormatSettings'])
        def classPath = matchOrCreateChild(settings, 'option', [name: 'implementationClassPath'])
        if (!uris.isPresent()) {
            // delete the classpath configuration
            classPath.replaceNode {}
        } else {
            def listItems = matchOrCreateChild(classPath, 'list')
            listItems.children().clear()
            uris.get().forEach { URI uri ->
                listItems.appendNode('option', [value: uri])
            }
        }
    }

    private static Node matchOrCreateChild(Node base, String name, Map attributes = [:], Map defaults = [:]) {
        def child = base[name].find { it.attributes().entrySet().containsAll(attributes.entrySet()) }
        if (child) {
            return child
        }

        return base.appendNode(name, attributes + defaults)
    }
}
