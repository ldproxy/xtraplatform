import de.ii.xtraplatform.base.test.Slow

runner {
    if (!System.properties['spock.include.Slow']) {
        exclude {
            annotation Slow
        }
    }
}