package com.palantir.javaformat.gradle

class ConfigureJavaFormatterXml {
    static void configureJavaFormat(Node rootNode, List<URI> uris) {
        def settings = matchOrCreateChild(rootNode, 'component', [name: 'PalantirJavaFormatSettings'])
        // enable
        matchOrCreateChild(settings, 'option', [name: 'enabled']).attributes().put('value', 'true')
        // configure classpath
        def classPath = matchOrCreateChild(settings, 'option', [name: 'implementationClassPath'])
        def listItems = matchOrCreateChild(classPath, 'list')
        listItems.children().clear()
        uris.forEach { URI uri ->
            listItems.appendNode('option', [value: uri])
        }
    }

    static void configureExternalDependencies(Node rootNode) {
        def externalDependencies = matchOrCreateChild(rootNode, 'component', [name: 'ExternalDependencies'])
        matchOrCreateChild(externalDependencies, 'plugin', [id: 'palantir-java-format'])
    }

    private static Node matchOrCreateChild(Node base, String name, Map attributes = [:], Map defaults = [:]) {
        def child = base[name].find { it.attributes().entrySet().containsAll(attributes.entrySet()) }
        if (child) {
            return child
        }

        return base.appendNode(name, attributes + defaults)
    }
}
