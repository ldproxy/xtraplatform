import React, { Component } from 'react';
import { connect } from 'react-redux'


const mapStateToProps = (state /*, props*/ ) => {
    return {

    }
}

const ServiceEdit = () => (
    <div>
        <h2>ServiceEdit</h2>
    </div>
)



const ConnectedServiceEdit = connect(mapStateToProps)(ServiceEdit)

export default ConnectedServiceEdit