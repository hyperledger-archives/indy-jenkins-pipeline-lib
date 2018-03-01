import groovy.transform.Field
@Field public static def state = [:]

def call() {
    if (!state) {
        state.isTested = null
    }

    return state
}
