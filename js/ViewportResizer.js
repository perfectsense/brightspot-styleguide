/* eslint-disable no-unused-vars */
import Bliss from 'bliss'
import Util from './util.js'
/* global $, $$ */

export class ViewportResizer {
  get selectors () {
    return this.settings.selectors
  }

  get startX () {
    return this._startX
  }

  set startX (X) {
    this._startX = X
  }

  get startY () {
    return this._startY
  }

  set startY (Y) {
    this._startY = Y
  }
  get startWidth () {
    return this._startWidth
  }

  set startWidth (width) {
    this._startWidth = width
  }

  get startHeight () {
    return this._startHeight
  }

  set startHeight (height) {
    this._startHeight = height
  }

  get viewportWidth () {
    return this._viewportWidth
  }

  set viewportWidth (height) {
    this._viewportWidth = height
  }

  get viewportHeight () {
    return this._viewportHeight
  }

  set viewportHeight (height) {
    this._viewportHeight = height
  }

  get devices () {
    return this.settings.devices
  }

  constructor (ctx, options = {}) {
    this.$ctx = $(ctx)
    this.settings = Object.assign({}, {
      selectors: {
        controls: 'StyleguideViewport',
        example: 'StyleguideExample',
        iframe: 'StyleguideExample-frame'
      }
    }, options)
  }

  init () {
    let self = this
    // bind actions for viewport controls
    // bind width input field
    // init the viewport on load
    let $viewportResizer = this.$ctx.nextElementSibling.querySelector(`.${this.selectors.iframe}`)
    let $widthInput = this.$ctx.querySelector(`.${this.selectors.controls}-width`)
    let $heightInput = this.$ctx.querySelector(`.${this.selectors.controls}-height`)

    self.updateViewportContainer($viewportResizer)
    // also update the input field with width
    self.updateInputs($widthInput, $heightInput)

    $widthInput.addEventListener('focusout', function () {
      self.updateViewport({width: this.value})
    })
    // bind height input field
    $heightInput.addEventListener('focusout', function () {
      self.updateViewport({height: this.value})
    })
    // bind function to the reset button
    this.$ctx.querySelector(`.${this.selectors.controls}-reset`).addEventListener('click', () => {
      this.updateViewport({width: '', height: ''})
    })
    // bind function based on preset viewports
    this.$ctx.querySelectorAll('[data-viewportsize]').forEach(function (element) {
      element.addEventListener('click', function () {
        self.updateViewport(JSON.parse(this.getAttribute('data-viewportsize')))
      })
    })
    // bind to Styleguide:updateViewport eventlistener
    this.$ctx.addEventListener('Styleguide:updateViewport', function (event) {
      self.updateViewportContainer($viewportResizer)
      // also update the input field with width
      self.updateInputs($widthInput, $heightInput)
    })
    // listens for tab change event and removes or adds width of viewport
    this.$ctx.nextElementSibling.querySelector(`.${this.selectors.iframe}`).addEventListener('Styleguide:tabChange', function (event) {
      if (window.location.hash === '#example') {
        this.style.width = `${self.viewportWidth}px`
        this.style.height = `${self.viewportHeight}px`
      } else {
        this.removeAttribute('style')
      }
    })

    // bind viewport frame actions
    this.$ctx.nextElementSibling._.contents($.create('div', {
      className: `${this.selectors.example}-handle`
    }))

    this.$ctx.nextElementSibling.addEventListener('mousedown', function (event) {
      self.initDrag(event)
      this.setAttribute('data-resizable', '')
    }, false)
  }

  initDrag (event) {
    this.startX = event.clientX
    this.startY = event.clientY
    let $resizer = this.$ctx.nextElementSibling.querySelector(`.${this.selectors.iframe}`)

    let computedStyle = document.defaultView.getComputedStyle($resizer)
    this.startWidth = parseInt(computedStyle.width, 10)
    this.startHeight = parseInt(computedStyle.height, 10)

    let doc = document.documentElement
    doc.addEventListener('mousemove', (event) => {
      this.dragContainer(event, $resizer)
    }, false)

    doc.addEventListener('mouseup', () => {
      this.stopDrag($resizer)
    }, false)
  }

  dragContainer (event, $container) {
    this.viewportWidth = (this.startWidth + event.clientX - this.startX)
    this.viewportHeight = (this.startHeight + event.clientY - this.startY)
    $container.style.width = `${this.viewportWidth}px`
    $container.style.height = `${this.viewportHeight}px`
  }

  stopDrag ($container) {
    $container.parentNode.removeAttribute('data-resizable')
    document.documentElement._.unbind('mousemove')
    document.documentElement._.unbind('mouseup')
    this.updateViewport({width: this.viewportWidth, height: this.viewportHeight})
  }

  updateInputs ($viewportWidthInput, $viewportHeightInput) {
    if (this.viewportWidth !== '') {
      $viewportWidthInput.value = this.viewportWidth
    } else {
      $viewportWidthInput.value = ''
    }

    if (this.viewportHeight !== '') {
      $viewportHeightInput.value = this.viewportHeight
    } else {
      $viewportHeightInput.value = ''
    }
  }
  updateViewportContainer ($resizer) {
    let search = Util.locationSearchToObject(window.location.search)
    // set the active state of the viewport buttons based on what width and height are set in the search url
    this.$ctx.querySelectorAll('button').forEach(function (element) {
      let viewportsize = element.getAttribute('data-viewportsize')
      element.removeAttribute('data-active')

      if (viewportsize === null) {
        if (search['width'] === undefined && search['height'] === undefined) {
          element.setAttribute('data-active', '')
        }
        return
      }

      let viewportDimensions = JSON.parse(viewportsize)
      if (viewportDimensions.width === search['width'] && viewportDimensions.height === search['height']) {
        element.setAttribute('data-active', '')
      }
    })

    // set the width of the iframe
    if (search['width']) {
      this.viewportWidth = search['width']
      $resizer.style.width = `${this.viewportWidth}px`
      $resizer.parentNode.setAttribute('data-viewportset', '')
    } else {
      this.viewportWidth = ''
      $resizer.style.width = this.viewportWidth
      $resizer.parentNode.removeAttribute('data-viewportset')
    }

    // set the height of the iframe
    if (search['height']) {
      this.viewportHeight = search['height']
      $resizer.style.height = `${this.viewportHeight}px`
    } else {
      this.viewportHeight = ''
      $resizer.style.height = this.viewportHeight
    }
  }

  updateViewport (deviceViewport) {
    let baseUrl = Util.updateSearchURL(window.location, deviceViewport)
    window.history.pushState({}, 'Update DeviceViewport', baseUrl)
    // set an event for viewport change
    let updateViewportEvent = document.createEvent('Event')
    updateViewportEvent.initEvent('Styleguide:updateViewport', false, true)
    this.$ctx.dispatchEvent(updateViewportEvent)
  }
}
