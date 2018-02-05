define([
    'react',
    'react-dom',
    'create-react-class',
    'prop-types',
    'popper'
], function(
    React,
    ReactDOM,
    createReactClass,
    PropTypes,
    Popper) {

    var container;

    const TriggerTypes = { Focus: 'focus', Manual: 'manual' };
    const eventsForTrigger = {
        [TriggerTypes.Focus]: ['focus', 'blur'],
        [TriggerTypes.Manual]: []
    };

    const Tooltip = createReactClass({

        propTypes: {
            node: PropTypes.instanceOf(HTMLElement),
            message: PropTypes.string.isRequired,
            trigger: PropTypes.string.isRequired,
            active: PropTypes.bool,
            className: PropTypes.string,
            subtitle: PropTypes.string,
            footer: PropTypes.string,
            placement: PropTypes.string,
            offset: PropTypes.string,
            allowOverflow: PropTypes.array
        },

        getDefaultProps() {
            return { trigger: TriggerTypes.Manual, placement: 'auto' }
        },

        getInitialState() {
            return { style: { visibility: 'hidden' } }
        },

        componentWillMount() {
            this.el = document.createElement('div');
            $(document).on('graphPaddingUpdated', this.onGraphPaddingUpdated);
        },

        componentDidMount() {
            getContainer().appendChild(this.el);
            if (this.props.active) {
                this.toggle(true)
            }
        },

        componentWillReceiveProps(nextProps) {
            const { node: prevNode, trigger: prevTrigger } = this.props;
            const { node, trigger } = nextProps;

            if (node !== prevNode || prevTrigger !== trigger) {
                if (prevTrigger && prevNode) {
                    eventsForTrigger[prevTrigger].forEach(event => {
                        prevNode.removeEventListener(event, this.onHandleEvent, false);
                    })
                }
                if (trigger && node) {
                    eventsForTrigger[trigger].forEach(event => {
                        node.addEventListener(event, this.onHandleEvent, false);
                    })
                }
            }
        },

        componentDidUpdate() {
            const { trigger, active } = this.props;
            if (trigger === TriggerTypes.Manual) {
                this.toggle(Boolean(this.props.active));
            }
        },

        componentWillUnmount() {
            const { node, trigger } = this.props;

            if (node && trigger) {
                eventsForTrigger[trigger].forEach(event => {
                    node.removeEventListener(event, this.onHandleEvent, false);
                })
            }
            if (this.popper) {
                this.popper.destroy();
            }
            getContainer().removeChild(this.el);
            $(document).off('graphPaddingUpdated', this.onGraphPaddingUpdated);
        },

        render() {
            const { node, message, subtitle, footer, placement: initialPlacement, className = '' } = this.props;
            const { style, placement } = this.state;
            const cls = `tooltip fade ${placement || initialPlacement || ''} in ${className}`.trim();

            // Mirror the bootstrap tooltip dom structure
            return ReactDOM.createPortal(
                (
                    <div ref={r => {this.tooltip = r}} className={cls} style={style}>
                        <div className="tooltip-arrow"></div>
                        <div className="tooltip-inner">
                            <div className="message">{message}</div>
                            { subtitle ? (<div className="subtitle">{subtitle}</div>) : null }
                            { footer ? (<div className="footer">{footer}</div>) : null }
                        </div>
                    </div>
                ),
                this.el
            );
        },

        onHandleEvent(event) {
            const { node, trigger } = this.props;

            if (node && trigger === TriggerTypes.Focus) {
                const show = event.type === 'focus';
                this.toggle(show);
            }
        },

        toggle(show) {
            const { node, placement, allowOverflow = [], offset = 0 } = this.props;
            const { style } = this.state;
            const visibility = show ? undefined : 'hidden';
            const priority = ['left', 'right', 'top', 'bottom'];

            if (style.visibility !== visibility) {

                if (this.popper) {
                    this.popper.destroy();
                    this.popper = null;
                }

                if (show) {
                    this.popper = new Popper(node, this.tooltip, {
                        placement: placement,
                        modifiers: {
                            offset: { offset },
                            preventOverflow: {
                                priority: allowOverflow.length ?
                                    _.without(priority, ...allowOverflow) :
                                    priority
                            },
                            applyStyle: { enabled: false },
                            applyReactStyle: {
                                enabled: true,
                                order: 900,
                                fn: ({ styles, placement }) => {
                                    this.setState({ placement, style: styles })
                                }
                            }
                        }
                    })
                } else {
                    this.setState({ style: { visibility } })
                }
            }
        },

        onGraphPaddingUpdated() {
            if (this.popper) {
                this.popper.scheduleUpdate();
            }
        }

    })

    return Tooltip;

    function getContainer() {
        const id = 'jsx-tooltip-container';
        if (!container) {
            container = document.getElementById(id);
            if (!container) {
                container = document.createElement('div');
                container.id = id;
                document.body.appendChild(container)
            }
        }
        return container;
    }
})
