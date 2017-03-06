/* eslint-disable no-unused-vars */
import Bliss from 'bliss'
import Util from './util.js'
/* global $, $$ */

export class DeviceViewport {
  get selectors () {
    return this.settings.selectors
  }

  get devices () {
    return this.settings.devices
  }

  constructor (ctx, options = {}) {
    this.ctx = ctx
    this.settings = Object.assign({}, {
      selectors: {
        deviceContainer: 'StyleguideDevices',
        viewport: 'StyleguideExample'
      }
    }, options)
  }

  init () {
    let self = this
    // bind width input field
    $(`.${this.selectors.deviceContainer}-width`).addEventListener('focusout', function () {
      self.updateDeviceViewport({width: this.value})
    })
    // bind height input field
    $(`.${this.selectors.deviceContainer}-height`).addEventListener('focusout', function () {
      self.updateDeviceViewport({height: this.value})
    })
    // bind function to the reset button
    $(`.${this.selectors.deviceContainer}-reset`).addEventListener('click', () => {
      this.updateDeviceViewport({width: '', height: ''})
    })
    // bind function based on preset viewports
    $$('[data-viewportsize]').forEach(function (element) {
      element.addEventListener('click', function () {
        self.updateDeviceViewport(JSON.parse(this.getAttribute('data-viewportsize')))
      })
    })
    // bind to Styleguide:updateViewport eventlistener
    $(`.${this.selectors.deviceContainer}`).addEventListener('Styleguide:updateViewport', function (event) {
      self.updateViewport()
    })
    // init the viewport
    self.updateViewport()
  }
  updateViewport () {
    let search = Util.locationSearchToObject(window.location.search)
    // set the active state of the viewport buttons based on what width and height are set in the search url
    $$(`.${this.selectors.deviceContainer} button`).forEach(function (element) {
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
    // also update the input field with width
    let $viewport = $(`.${this.selectors.viewport}`)
    let $inputWidth = $(`.${this.selectors.deviceContainer}-width`)
    if (search['width']) {
      $viewport.style.width = `${search['width']}px`
      $inputWidth.value = search['width']
    } else {
      $viewport.style.width = ''
      $inputWidth.value = ''
    }

    // set the height of the iframe
    // also update the input field with height
    let $inputHeight = $(`.${this.selectors.deviceContainer}-height`)
    if (search['height']) {
      $viewport.style.height = `${search['height']}px`
      $inputHeight.value = search['height']
    } else {
      $viewport.style.height = ''
      $inputHeight.value = ''
    }
  }

  updateDeviceViewport (deviceViewport) {
    let baseUrl = this.updateSearchURL(window.location, deviceViewport)
    window.history.pushState({}, 'Update DeviceViewport', baseUrl)
    // set an event for viewport change
    let updateViewportEvent = document.createEvent('Event')
    updateViewportEvent.initEvent('Styleguide:updateViewport', false, true)
    $(`.${this.selectors.deviceContainer}`).dispatchEvent(updateViewportEvent)
  }

  updateSearchURL (locationObj, paramObj) {
    let url = locationObj.search
    let urlSearch = url.split('?')
    if (urlSearch.length >= 2) {
      let urlParams = urlSearch[1].split(/[&;]/g)
      let hashParam = ''
      let hash = locationObj.hash
      for (let param in paramObj) {
        let prefix = encodeURIComponent(param) + '='
        for (let i = 0; i < urlParams.length; i++) {
          if (urlParams[i].lastIndexOf(prefix, 0) !== -1) {
            urlParams.splice(i, 1)
          }
        }

        if (paramObj[param] !== '') {
          urlParams.push(`${param}=${paramObj[param]}`)
        }
      }
      urlSearch = `?${urlParams.join('&')}${locationObj.hash}`
    }
    return urlSearch
  }
}
