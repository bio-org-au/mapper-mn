import au.org.biodiversity.mapper.Identifier

mapper {
    resolverURL = 'http://localhost:7070/nsl-mapper'
    contextExtension = '' //extension to the context path (after nsl-mapper).
    defaultProtocol = 'http'
    Map serviceHosts = [
            bpni: 'http://bpni.com',
            apni: 'http://apni.com/nsl',
            ausmoss: 'http://ausmoss.com/nsl',
            algae: 'http://algae.net/thing',
            fungi: 'http://fungi.foo',
            foa : 'http://test.biodiversity.org.au/'
    ]

    Closure htmlResolver = { Identifier ident ->
        String host = serviceHosts[ident.nameSpace]
        if (ident.objectType == 'treeElement') {
            return "${host}/services/rest/${ident.objectType}/${ident.versionNumber}/${ident.idNumber}"
        }
        return "${host}/services/rest/${ident.objectType}/${ident.nameSpace}/${ident.idNumber}"
    }

    Closure jsonResolver = { Identifier ident ->
        String host = serviceHosts[ident.nameSpace]
        if (ident.objectType == 'treeElement') {
            return "${host}/services/json/${ident.objectType}/${ident.versionNumber}/${ident.idNumber}"
        }
        return "${host}/services/json/${ident.objectType}/${ident.nameSpace}/${ident.idNumber}"
    }

    format {
        html = htmlResolver
        json = jsonResolver
        xml = htmlResolver
        rdf = {Identifier ident -> return null}
    }

    auth = [
            'TEST-services': [
                    secret: 'buy-me-a-pony',
                    application: 'services',
                    roles      : ['admin'],
            ],
            'TEST-editor': [
                    secret: 'I-am-a-pony',
                    application: 'editor',
                    roles      : ['admin'],
            ]
    ]
}
