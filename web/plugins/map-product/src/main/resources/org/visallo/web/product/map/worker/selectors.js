define(['reselect', 'data/web-worker/store/configuration/selectors'], function({ createSelector }, configSelectors) {

    const getProviderProperties = createSelector([configSelectors.getProperties], (properties) => {
        return _.pick(properties, (value, property) => property.startsWith('map.provider'))
    });

    const getDefaultProvider = createSelector([getProviderProperties], (providerProperties) => {
        return providerProperties['map.provider'] || 'osm';
    })

    const getProviders = createSelector([getProviderProperties, getDefaultProvider], (providerProperties, defaultProvider) => {
        const providers = _.chain(providerProperties)
            .pick((value, property) => property.startsWith('map.provider.'))
            .reduce((providers, value, key) => {
                const [ str, provider ] = key.match(/^map\.provider\.(.+)\..*$/);

                if (provider && !providers[provider]) {
                    providers[provider] = { providerName: provider, label: provider };
                }
                if (key.endsWith('.label')) {
                    providers[provider].label = value;
                }
                if (key.endsWith('.source')) {
                    providers[provider].providerSource = value;
                }

                return providers;
            }, { [defaultProvider]: { providerName: defaultProvider, label: defaultProvider }})
            .values()
            .value();

        return providers
    })


    return {
        getProviderProperties,
        getProviders,
        getDefaultProvider
    }
})
