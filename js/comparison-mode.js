/* global $$ */
import Util from './util.js'

export default {
  init: () => {
    $$(`.StyleguideExample-frame`)._.addEventListener(`load`, () => {
      $$(`.StyleguideComparison-frame`).forEach(el => {
        el.src = 'about:blank'
      })
    })

    $$(`.StyleguideExample-frame`)._.addEventListener(`load`, () => {
      const examplePath = Util.locationSearchToObject(window.location.search).file
      $$(`.StyleguideComparison-group`).forEach(el => {
        el.removeAttribute(`data-visible`)
        if (el.dataset.examplePath && el.dataset.examplePath === examplePath) {
          el.setAttribute(`data-visible`, ``)
        }
      })
    })
  }
}
