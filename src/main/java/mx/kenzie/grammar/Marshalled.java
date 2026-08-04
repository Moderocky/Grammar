package mx.kenzie.grammar;

/// Something that can be stored in a map of simple types.
/// Objects of this type can always be passed to a grammar without needing registration.
///
/// No grammar instance is made available during the marshalling call--
/// extenders are expected to handle their own values, rather than relying on whatever else
/// is available in the ecosystem.
/// Types requiring more configuration should register a marshalling strategy.
public interface Marshalled {

    Container marshal() throws GrammarException;

    /// An object that can be resolved from a map of simple types.
    /// This is only useful for mutable objects.
    /// Immutable objects or complex types should register an unmarshalling strategy.
    ///
    /// There is a general expectation that [#unmarshal(mx.kenzie.grammar.Container)] will
    /// be called only once after object creation.
    ///
    /// The constructor of an unmarshalled class may not be called during creation.
    interface Unmarshalled extends Marshalled {

        /// Initialises (updates) this object with the values inside the container.
        void unmarshal(Container data) throws GrammarException;

    }

}
