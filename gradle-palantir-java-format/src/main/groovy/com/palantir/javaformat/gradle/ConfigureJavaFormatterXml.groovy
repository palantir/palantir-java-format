package com.palantir.javaformat.gradle

class ConfigureJavaFormatterXml {
    static void configure(Node rootNode, List<URI> uris) {
        def settings = matchOrCreateChild(rootNode, 'component', [name: 'PalantirJavaFormatSettings'])
        def classPath = matchOrCreateChild(settings, 'option', [name: 'implementationClassPath'])
        def listItems = matchOrCreateChild(classPath, 'list')
        listItems.children().clear()
        uris.forEach { URI uri ->
            listItems.appendNode('option', [value: uri])
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
