/* global $, $$ */
import Util from './util.js'

export default {
  init: () => {
    let compareMode = 0

    $$(`.StyleguideExample-frame`)._.addEventListener(`load`, (evt) => {
      const examplePath = Util.locationSearchToObject(window.location.search).file
      const exampleFrame = evt.currentTarget

      exampleFrame._.style({
        width: `100%`,
        height: `100%`,
        margin: `0px`
      })

      if (compareMode === 0) {
        $(`.StyleguideComparison-modeButton`).textContent = `Overlay`
      } else {
        $(`.StyleguideComparison-modeButton`).textContent = `Side-by-Side`
      }

      $$(`.StyleguideComparison-frame`).forEach(el => { el.setAttribute(`src`, `about:blank`) })

      $$(`.StyleguideComparison-group`).forEach(el => {
        el.removeAttribute(`data-visible`)
        if (el.dataset.examplePath && el.dataset.examplePath === examplePath) {
          el.setAttribute(`data-visible`, ``)
          el.querySelectorAll(`a`).forEach(button => {
            button.removeEventListener(`click`, onDesignChanged)
            button.addEventListener(`click`, onDesignChanged)
          })
        }
      })

      $(`#slider-opacity`).addEventListener(`input`, (evt) => {
        const newOpacity = evt.currentTarget.value
        $('.OpacityControl-output').value = newOpacity
        $(`.StyleguideExample-frame`)._.style({
          opacity: newOpacity
        })
      })

      if ($$(`.StyleguideComparison-group[data-visible]`).length > 0) {
        $(`.StyleguideComparison-controls`).setAttribute(`data-visible`, ``)
        $(`.StyleguideComparison-modeButton`).addEventListener(`click`, updateMode)
      } else {
        $(`.StyleguideComparison-controls`).removeAttribute(`data-visible`, ``)
        $(`.StyleguideComparison-modeButton`).removeEventListener(`click`, updateMode)
      }

      $(`.StyleguideExample`).removeAttribute(`data-comparing`)
    })

    let onDesignChanged = (evt) => {
      const button = evt.currentTarget

      evt.preventDefault()

      // Reset states.
      $(`.StyleguideComparison-frame`).setAttribute(`src`, `about:blank`)
      button.removeEventListener(`animationend`)

      // Toggle the button.
      if (button.hasAttribute(`data-active`)) {
        button.removeAttribute(`data-active`)

        // Disable comparison mode.
        $(`.StyleguideExample`).removeAttribute(`data-comparing`)
        $(`.StyleguideComparison-frame`).classList.remove('animate')
        $(`.StyleguideExample-frame`).classList.remove('animate')
        $(`.StyleguideExample-frame`).setAttribute(`style`, ``)
      } else {
        // Remove previous active state.
        $$(`.StyleguideComparison-group a`).forEach(el => {
          if (el.hasAttribute(`data-active`)) {
            el.removeAttribute(`data-active`)
          }
        })

        // Activate this button.
        button.setAttribute(`data-active`, ``)

        compare()
      }
    }

    let compare = () => {
      const activeButton = $(`.StyleguideComparison-controls [data-active]`)
      const targetSrc = activeButton.getAttribute(`href`)
      const targetWidth = activeButton.dataset.targetWidth
      const targetHeight = activeButton.dataset.targetHeight

      // Enable comparison mode.
      $(`.StyleguideExample`).setAttribute(`data-comparing`, ``)

      // Immediately resize & position the comparison frame to the same coordinates as the example frame.
      $(`.StyleguideComparison-wrapper`)._.style({
        transitionDuration: '0ms',
        transform: `translateX(0px)`
      })

      $(`.StyleguideComparison-frame`)._.attributes({
        width: targetWidth,
        height: targetHeight
      })._.style({
        width: `${targetWidth}px`,
        height: `${targetHeight}px`
      })

      if (compareMode === 0) {
        $(`.StyleguideComparison-wrapper`)._.setAttribute(`data-transition-state`, `in-progress`)

        // Transition the comparison frame into visibility
        $(`.StyleguideExample-frame`)
        ._.transition({
          width: `${targetWidth}px`,
          height: `${targetHeight}px`,
          margin: `17px`
        }, 200)
        .then(el => {
          el.classList.remove('animate')

          // animate the example frame
          setTimeout(() => { el.classList.add('animate') }, 2)

          // slide the comparison frame out
          el.addEventListener(`animationend`, (evt) => {
            if (evt.animationName === `drop`) {
              const newX = parseInt(targetWidth) + 17

              el.classList.remove('animate')

              $(`.StyleguideComparison-wrapper`)._.transition({ transform: `translateX(${newX}px)` }, 200)
              .then(el => {
                $(`.StyleguideComparison-frame`).setAttribute(`src`, targetSrc)
                el.setAttribute(`data-transition-state`, `in-complete`)
              })
            }
          })
        })
      // Overlay mode
      } else {
        $(`.StyleguideComparison-wrapper`)._.transition({ transform: `translateX(0px)` }, 200)
        .then(el => {
          $(`.StyleguideComparison-frame`).setAttribute(`src`, targetSrc)
          el.setAttribute(`data-transition-state`, `in-complete`)
        })
      }
    }

    let updateMode = () => {
      if (compareMode === 0) {
        compareMode = 1
        $(`.StyleguideComparison-modeButton`).textContent = `Side-by-Side`
      } else {
        compareMode = 0
        $(`.StyleguideComparison-modeButton`).textContent = `Overlay`
      }

      $(`.StyleguideExample`).setAttribute(`data-compare-mode`, `${compareMode}`)
      compare()
    }
  }
}
