/* global $$ */
import Util from './util.js'

export default {
  init: () => {
    $$(`.StyleguideExample-frame`)._.addEventListener(`load`, () => {
      const examplePath = Util.locationSearchToObject(window.location.search).file

      $$(`.StyleguideComparison-frame`).forEach(el => {
        el.src = 'about:blank'
      })

      $$(`.StyleguideComparison-group`).forEach(el => {
        el.removeAttribute(`data-visible`)
        if (el.dataset.examplePath && el.dataset.examplePath === examplePath) {
          el.setAttribute(`data-visible`, ``)
        }

        el.querySelectorAll(`a`).forEach(el => {
          el.addEventListener(`click`, () => {
            $$(`.StyleguideExample-frame`)._.attributes({
              width: el.dataset.targetWidth,
              height: el.dataset.targetHeight
            })
          })
        })
      })
    })
  }
}
