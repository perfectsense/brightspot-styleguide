module.exports = string => string
  .replace(/\W+/g, ' ')
  .replace(/([a-z])([A-Z])/g, '$1 $2')
  .trim()
  .split(' ')
  .filter(part => !!part)
  .map(part => part[0].toUpperCase() + part.slice(1))
  .join(' ')
