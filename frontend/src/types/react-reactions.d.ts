// Type declaration for react-reactions/src/helpers/icons
// This module exports a helper with a `find(scope, name)` method
// that returns a data-URI string for a reaction image icon.

declare module 'react-reactions/src/helpers/icons' {
  const icons: {
    find(scope: string, name: string): string;
  };
  export default icons;
}
