define(['reselect'], function({ createSelector }) {

    const getConfig = (state) => state.configuration;

    const getMessages = createSelector([getConfig], config => config.messages || {})
    const getProperties = createSelector([getConfig], config => config.properties || {})

    return {
        getConfig,
        getMessages,
        getProperties
    }
})
